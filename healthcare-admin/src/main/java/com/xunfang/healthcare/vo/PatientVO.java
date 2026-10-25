package com.xunfang.healthcare.vo;

import com.xunfang.healthcare.entity.BizPatient;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 患者档案展示对象：身份证号与手机号解密后返回，同时提供脱敏值。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "患者档案")
public class PatientVO extends BizPatient {

    private static final long serialVersionUID = 1L;

    @Schema(description = "年龄（由出生日期推算）")
    private Integer age;

    @Schema(description = "脱敏后的身份证号")
    private String idCardMasked;

    @Schema(description = "脱敏后的手机号")
    private String phoneMasked;
}
