package com.xunfang.healthcare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 密码重置与修改请求。
 */
@Data
@Schema(description = "密码重置与修改请求")
public class PasswordRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户 ID，管理员重置时必填")
    private Long userId;

    @NotBlank(message = "请输入原密码")
    @Schema(description = "原密码，修改密码时必填")
    private String oldPassword;

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 20, message = "密码长度需在 6-20 位之间")
    @Schema(description = "新密码")
    private String newPassword;
}
