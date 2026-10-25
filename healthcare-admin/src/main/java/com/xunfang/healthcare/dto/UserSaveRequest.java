package com.xunfang.healthcare.dto;

import com.xunfang.healthcare.entity.SysUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 用户保存请求：用户主体 + 角色 ID 列表 + 初始密码。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户保存请求")
public class UserSaveRequest extends SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色 ID 列表")
    private List<Long> roleIds;

    @Schema(description = "初始密码，新增时若为空则使用默认密码 123456")
    private String rawPassword;
}
