package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库房表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_warehouse")
public class BizWarehouse extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 库房编码。
     */
    private String warehouseCode;

    /**
     * 库房名称。
     */
    private String warehouseName;

    /**
     * 所属科室。
     */
    private Long deptId;

    /**
     * 库房类型 1门诊药房2住院药房3中心库房。
     */
    private Integer warehouseType;

    /**
     * 负责人。
     */
    private String manager;

    /**
     * 联系电话。
     */
    private String phone;

    /**
     * 地址。
     */
    private String address;

    /**
     * 状态 0停用1启用。
     */
    private Integer status;
}
