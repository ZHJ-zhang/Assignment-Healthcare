package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.dto.LoginRequest;
import com.xunfang.healthcare.dto.LoginResponse;
import com.xunfang.healthcare.dto.UserInfoVO;
import com.xunfang.healthcare.service.AuthService;
import com.xunfang.healthcare.vo.RouterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 认证相关接口：登录、登出、当前用户信息与动态路由。
 */
@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 登录。
     */
    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return Result.success(authService.login(request, httpRequest));
    }

    /**
     * 登出，令牌立即失效。
     */
    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        authService.logout(resolveToken(request));
        return Result.success();
    }

    /**
     * 当前登录用户信息。
     */
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/getInfo")
    public Result<UserInfoVO> getInfo() {
        return Result.success(authService.getInfo());
    }

    /**
     * 当前用户的动态路由。
     */
    @Operation(summary = "获取动态路由")
    @GetMapping("/getRouters")
    public Result<List<RouterVO>> getRouters() {
        return Result.success(authService.getRouters());
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return request.getParameter("token");
    }
}
