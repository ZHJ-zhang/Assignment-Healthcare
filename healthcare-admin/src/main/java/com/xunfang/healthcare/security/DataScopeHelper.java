package com.xunfang.healthcare.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.xunfang.healthcare.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据权限助手（行级权限）。
 * <p>按钮权限管"能不能调这个接口"，数据权限管"调到之后能看见谁的数据"。
 * 取值与 {@code sys_role.data_scope} 一致：1 全部、2 本部门及以下、3 本部门、4 仅本人。</p>
 *
 * <p><b>当前只启用"仅本人"这一档</b>，也就是 {@code data_scope = 4} 的账号——
 * 现实中只有医生会被设成这一档：医生只该看到挂在自己名下的就诊、处方、病历与检查申请，
 * 跨科室、跨医生的病患对他不可见。之所以不启用 2/3 档，是因为收费处、药房这些角色
 * 的 data_scope 也是 2（本部门及以下），若照此过滤，收费员会被限制在"住院收费处"这个科室里，
 * 连一条挂号都查不到。等科室层级真正用起来再放开。</p>
 */
@Component
@RequiredArgsConstructor
public class DataScopeHelper {

    public static final int SCOPE_ALL = 1;
    public static final int SCOPE_DEPT_AND_CHILD = 2;
    public static final int SCOPE_DEPT = 3;
    public static final int SCOPE_SELF = 4;

    private final SysDeptMapper sysDeptMapper;

    /**
     * 当前登录用户的原始数据范围。
     */
    public int scope() {
        LoginUser loginUser = SecurityUtils.getLoginUser().orElse(null);
        if (loginUser == null || loginUser.isAdmin()) {
            return SCOPE_ALL;
        }
        Integer scope = loginUser.getDataScope();
        return scope == null ? SCOPE_ALL : scope;
    }

    /**
     * 是否只能看本人的数据。
     */
    public boolean onlySelf() {
        return scope() == SCOPE_SELF;
    }

    /**
     * 仅在"仅本人"生效时返回当前用户 ID，否则返回 null——方便直接塞进 {@code eq} 的可选条件。
     */
    public Long scopeUserId() {
        return onlySelf() ? SecurityUtils.getUserIdOrNull() : null;
    }

    /**
     * 本部门及下级的科室 ID，供"本部门及以下"档位使用（当前未启用，留给科室层级细化时接线）。
     */
    public List<Long> scopeDeptIds() {
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            return List.of();
        }
        return sysDeptMapper.selectSelfAndDescendantIds(deptId);
    }

    /**
     * 给业务查询追加数据权限条件。
     *
     * @param wrapper     条件构造器
     * @param ownerColumn 业务表里"归属医生"的字段（如 {@code BizPrescription::getDoctorId}）
     */
    public <T> void applySelf(LambdaQueryWrapper<T> wrapper, SFunction<T, ?> ownerColumn) {
        if (wrapper == null || ownerColumn == null || !onlySelf()) {
            return;
        }
        Long userId = SecurityUtils.getUserIdOrNull();
        if (userId == null) {
            return;
        }
        wrapper.eq(ownerColumn, userId);
    }

    /**
     * 详情类接口的越权校验：仅本人档位下，访问别人名下的数据直接拒绝。
     *
     * @param ownerId 数据归属的医生 ID
     * @param what    数据名称，用于提示，例如"处方"
     */
    public void checkOwner(Long ownerId, String what) {
        if (!onlySelf()) {
            return;
        }
        Long userId = SecurityUtils.getUserIdOrNull();
        if (userId == null || ownerId == null || !userId.equals(ownerId)) {
            throw new com.xunfang.healthcare.common.BizException(
                    com.xunfang.healthcare.common.ResultCode.FORBIDDEN.getCode(),
                    "无权查看他人的" + what);
        }
    }
}
