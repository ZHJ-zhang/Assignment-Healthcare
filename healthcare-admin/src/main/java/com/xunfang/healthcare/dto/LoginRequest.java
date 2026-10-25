package com.xunfang.healthcare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录请求。
 */
@Data
@Schema(description = "登录请求")
public class LoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "请输入账号")
    @Schema(description = "登录账号", example = "admin")
    private String username;

    @NotBlank(message = "请输入密码")
    @Schema(description = "登录密码", example = "123456")
    private String password;
}
