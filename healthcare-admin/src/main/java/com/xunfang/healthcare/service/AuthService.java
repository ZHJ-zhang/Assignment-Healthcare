package com.xunfang.healthcare.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xunfang.healthcare.common.BizException;
import com.xunfang.healthcare.common.ResultCode;
import com.xunfang.healthcare.config.properties.JwtProperties;
import com.xunfang.healthcare.dto.LoginRequest;
import com.xunfang.healthcare.dto.LoginResponse;
import com.xunfang.healthcare.dto.UserInfoVO;
import com.xunfang.healthcare.entity.SysDept;
import com.xunfang.healthcare.entity.SysLoginLog;
import com.xunfang.healthcare.entity.SysMenu;
import com.xunfang.healthcare.entity.SysSecurityConfig;
import com.xunfang.healthcare.entity.SysUser;
import com.xunfang.healthcare.mapper.SysDeptMapper;
import com.xunfang.healthcare.mapper.SysLoginLogMapper;
import com.xunfang.healthcare.mapper.SysMenuMapper;
import com.xunfang.healthcare.mapper.SysSecurityConfigMapper;
import com.xunfang.healthcare.mapper.SysUserMapper;
import com.xunfang.healthcare.security.DataScopeHelper;
import com.xunfang.healthcare.security.JwtTokenProvider;
import com.xunfang.healthcare.security.LoginUser;
import com.xunfang.healthcare.security.SecurityUtils;
import com.xunfang.healthcare.security.TokenBlacklist;
import com.xunfang.healthcare.security.UserDetailsServiceImpl;
import com.xunfang.healthcare.vo.RouterVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 登录认证、当前用户信息与动态路由。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String FAIL_KEY_PREFIX = "healthcare:login:fail:";
    private static final String LOCK_KEY_PREFIX = "healthcare:login:lock:";

    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final TokenBlacklist tokenBlacklist;
    private final StringRedisTemplate stringRedisTemplate;
    private final SysUserMapper sysUserMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysLoginLogMapper sysLoginLogMapper;
    private final SysSecurityConfigMapper sysSecurityConfigMapper;

    /**
     * 登录。
     */
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        SysSecurityConfig securityConfig = loadSecurityConfig();
        String username = request.getUsername().trim();
        String ip = resolveIp(httpRequest);

        checkIpWhitelist(securityConfig, ip);
        checkLocked(username, securityConfig);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword()));
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            clearFailCount(username);

            String token = tokenProvider.createToken(loginUser);
            updateLoginTime(loginUser.getUserId());
            saveLoginLog(username, ip, httpRequest, true, "登录成功");

            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setExpiresIn(tokenProvider.getExpireSeconds());
            response.setRoles(loginUser.getRoles());
            response.setUserInfo(buildUserInfo(loginUser));
            return response;
        } catch (DisabledException e) {
            saveLoginLog(username, ip, httpRequest, false, "账号已禁用");
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "账号已禁用，请联系管理员");
        } catch (BadCredentialsException e) {
            int failCount = increaseFailCount(username, securityConfig);
            saveLoginLog(username, ip, httpRequest, false, "密码错误");
            throw new BizException(ResultCode.BAD_REQUEST.getCode(),
                    "账号或密码错误，已连续失败 " + failCount + " 次");
        } catch (AuthenticationException e) {
            saveLoginLog(username, ip, httpRequest, false, "登录失败");
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "登录失败：" + e.getMessage());
        }
    }

    /**
     * 登出，令牌加入黑名单。
     */
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            tokenBlacklist.add(token, tokenProvider.getExpireSeconds());
        }
    }

    /**
     * 当前登录用户信息。
     */
    public UserInfoVO getInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUserRequired();
        return buildUserInfo(loginUser);
    }

    /**
     * 当前用户的动态路由。
     */
    public List<RouterVO> getRouters() {
        LoginUser loginUser = SecurityUtils.getLoginUserRequired();
        List<SysMenu> menus = loginUser.isAdmin()
                ? sysMenuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .eq(SysMenu::getStatus, 1)
                .orderByAsc(SysMenu::getSort))
                : sysMenuMapper.selectMenusByUserId(loginUser.getUserId());
        return buildRouters(menus);
    }

    private UserInfoVO buildUserInfo(LoginUser loginUser) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(loginUser.getUserId());
        vo.setUsername(loginUser.getUsername());
        vo.setRealName(loginUser.getRealName());
        vo.setDeptId(loginUser.getDeptId());
        vo.setDataScope(loginUser.isAdmin()
                ? DataScopeHelper.SCOPE_ALL
                : (loginUser.getDataScope() == null ? DataScopeHelper.SCOPE_ALL : loginUser.getDataScope()));
        vo.setRoles(loginUser.getRoles());
        vo.setPermissions(loginUser.getPermissions());

        SysUser user = sysUserMapper.selectById(loginUser.getUserId());
        if (user != null) {
            vo.setGender(user.getGender());
            vo.setAvatar(user.getAvatar());
        }
        if (loginUser.getDeptId() != null) {
            SysDept dept = sysDeptMapper.selectById(loginUser.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getDeptName());
            }
        }
        return vo;
    }

    private List<RouterVO> buildRouters(List<SysMenu> menus) {
        List<SysMenu> visible = menus.stream()
                .filter(menu -> !"F".equals(menu.getMenuType()))
                .filter(menu -> menu.getStatus() == null || menu.getStatus() == 1)
                .filter(menu -> menu.getVisible() == null || menu.getVisible() == 1)
                .sorted((a, b) -> Integer.compare(nullSafe(a.getSort()), nullSafe(b.getSort())))
                .collect(Collectors.toList());

        Map<Long, List<SysMenu>> grouped = visible.stream()
                .collect(Collectors.groupingBy(menu -> menu.getParentId() == null ? 0L : menu.getParentId(),
                        LinkedHashMap::new, Collectors.toList()));

        List<RouterVO> routers = new ArrayList<>();
        for (SysMenu menu : grouped.getOrDefault(0L, List.of())) {
            RouterVO router = toRouter(menu, "");
            List<RouterVO> children = buildChildren(menu.getId(), grouped);
            if (!children.isEmpty()) {
                router.setChildren(children);
                router.setRedirect("noRedirect");
                if (router.getComponent() == null) {
                    router.setComponent("Layout");
                }
            }
            routers.add(router);
        }
        return routers;
    }

    private List<RouterVO> buildChildren(Long parentId, Map<Long, List<SysMenu>> grouped) {
        List<RouterVO> children = new ArrayList<>();
        for (SysMenu menu : grouped.getOrDefault(parentId, List.of())) {
            RouterVO router = toRouter(menu, "");
            List<RouterVO> grandChildren = buildChildren(menu.getId(), grouped);
            if (!grandChildren.isEmpty()) {
                router.setChildren(grandChildren);
                router.setRedirect("noRedirect");
            }
            children.add(router);
        }
        return children;
    }

    private RouterVO toRouter(SysMenu menu, String parentPath) {
        RouterVO router = new RouterVO();
        String fullPath = joinPath(parentPath, menu.getPath());
        router.setName(routeName(fullPath, menu.getMenuName()));
        router.setPath(menu.getPath());
        router.setComponent(menu.getComponent());
        RouterVO.MetaVO meta = new RouterVO.MetaVO();
        meta.setTitle(menu.getMenuName());
        meta.setIcon(menu.getIcon());
        meta.setHidden(menu.getVisible() != null && menu.getVisible() == 0);
        meta.setKeepAlive(menu.getIsCache() != null && menu.getIsCache() == 1);
        router.setMeta(meta);
        return router;
    }

    private String joinPath(String parentPath, String path) {
        if (path == null || path.isBlank()) {
            return parentPath;
        }
        if (parentPath == null || parentPath.isBlank()) {
            return path;
        }
        return parentPath + "/" + path;
    }

    private String routeName(String fullPath, String menuName) {
        String source = fullPath != null && !fullPath.isBlank() ? fullPath : menuName;
        if (source == null || source.isBlank()) {
            return "Route";
        }
        return source.replace("/", "_");
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private SysSecurityConfig loadSecurityConfig() {
        SysSecurityConfig config = sysSecurityConfigMapper.selectOne(
                Wrappers.<SysSecurityConfig>lambdaQuery().last("limit 1"));
        if (config == null) {
            config = new SysSecurityConfig();
            config.setLoginFailLimit(5);
            config.setLockMinutes(30);
            config.setSessionTimeout(30);
            config.setMaskEnabled(1);
        }
        return config;
    }

    private void checkIpWhitelist(SysSecurityConfig config, String ip) {
        String whitelist = config.getIpWhitelist();
        if (whitelist == null || whitelist.isBlank()) {
            return;
        }
        boolean allowed = java.util.Arrays.stream(whitelist.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .anyMatch(item -> ip != null && (ip.equals(item) || ip.startsWith(item)));
        if (!allowed) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "当前 IP 不在白名单内，禁止访问");
        }
    }

    private void checkLocked(String username, SysSecurityConfig config) {
        String key = LOCK_KEY_PREFIX + username;
        try {
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
                Long ttl = stringRedisTemplate.getExpire(key);
                long minutes = ttl == null || ttl < 0 ? config.getLockMinutes() : ttl / 60 + 1;
                throw new BizException(ResultCode.FORBIDDEN.getCode(),
                        "账号已被锁定，请 " + minutes + " 分钟后再试");
            }
        } catch (BizException e) {
            // 业务异常（账号锁定）正常抛出
            throw e;
        } catch (Exception e) {
            // Redis 不可用时降级：跳过锁定校验，保证登录主流程可用
            log.warn("Redis 不可用，跳过登录锁定校验: {}", e.getMessage());
        }
    }

    private int increaseFailCount(String username, SysSecurityConfig config) {
        int limit = config.getLoginFailLimit() == null ? 5 : config.getLoginFailLimit();
        int lockMinutes = config.getLockMinutes() == null ? 30 : config.getLockMinutes();
        String failKey = FAIL_KEY_PREFIX + username;
        try {
            Long count = stringRedisTemplate.opsForValue().increment(failKey);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(failKey, Duration.ofMinutes(lockMinutes));
            }
            if (count != null && count >= limit) {
                stringRedisTemplate.opsForValue().set(LOCK_KEY_PREFIX + username, "1",
                        Duration.ofMinutes(lockMinutes));
                stringRedisTemplate.delete(failKey);
                throw new BizException(ResultCode.FORBIDDEN.getCode(),
                        "连续登录失败 " + limit + " 次，账号已锁定 " + lockMinutes + " 分钟");
            }
            return count == null ? 1 : count.intValue();
        } catch (BizException e) {
            // 业务异常（账号锁定）正常抛出
            throw e;
        } catch (Exception e) {
            // Redis 不可用时降级：跳过失败计数，避免因缓存故障导致登录 500
            log.warn("Redis 不可用，跳过登录失败计数: {}", e.getMessage());
            return 1;
        }
    }

    private void clearFailCount(String username) {
        try {
            stringRedisTemplate.delete(FAIL_KEY_PREFIX + username);
            stringRedisTemplate.delete(LOCK_KEY_PREFIX + username);
        } catch (Exception e) {
            // Redis 不可用时降级：忽略清理失败
            log.warn("Redis 不可用，跳过登录失败计数清理: {}", e.getMessage());
        }
    }

    private void updateLoginTime(Long userId) {
        SysUser update = new SysUser();
        update.setId(userId);
        update.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(update);
    }

    private void saveLoginLog(String username, String ip, HttpServletRequest request,
                               boolean success, String message) {
        try {
            SysLoginLog entity = new SysLoginLog();
            entity.setUsername(username);
            entity.setIp(ip);
            entity.setLocation("本地");
            entity.setBrowser(resolveBrowser(request));
            entity.setOs(resolveOs(request));
            entity.setStatus(success ? 1 : 0);
            entity.setMsg(message);
            entity.setLoginTime(LocalDateTime.now());
            sysLoginLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("记录登录日志失败: {}", e.getMessage());
        }
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            int idx = ip.indexOf(',');
            return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        return ip != null && !ip.isBlank() ? ip : request.getRemoteAddr();
    }

    private String resolveBrowser(HttpServletRequest request) {
        String agent = request == null ? null : request.getHeader("User-Agent");
        if (agent == null) {
            return "Unknown";
        }
        if (agent.contains("Edg")) {
            return "Edge";
        }
        if (agent.contains("Chrome")) {
            return "Chrome";
        }
        if (agent.contains("Firefox")) {
            return "Firefox";
        }
        if (agent.contains("Safari")) {
            return "Safari";
        }
        return "Unknown";
    }

    private String resolveOs(HttpServletRequest request) {
        String agent = request == null ? null : request.getHeader("User-Agent");
        if (agent == null) {
            return "Unknown";
        }
        if (agent.contains("Windows")) {
            return "Windows";
        }
        if (agent.contains("Mac OS")) {
            return "macOS";
        }
        if (agent.contains("Android")) {
            return "Android";
        }
        if (agent.contains("iPhone") || agent.contains("iPad")) {
            return "iOS";
        }
        if (agent.contains("Linux")) {
            return "Linux";
        }
        return "Unknown";
    }
}
