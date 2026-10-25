package com.xunfang.healthcare.common;

/**
 * 收费明细项目类型（biz_charge_item.item_type）。
 * <p>
 * 1-4 与接口文档一致，5 为护理，6 为挂号费。
 * 挂号费单列一类，是为了让「挂号费」和「门诊诊疗费」在财务报表里分得开。
 */
public final class ChargeItemType {

    /** 药品。 */
    public static final int DRUG = 1;

    /** 诊疗。 */
    public static final int TREATMENT = 2;

    /** 检查检验。 */
    public static final int EXAM = 3;

    /** 床位。 */
    public static final int BED = 4;

    /** 护理。 */
    public static final int NURSING = 5;

    /** 挂号费。 */
    public static final int REGISTRATION = 6;

    private ChargeItemType() {
    }

    /**
     * 类型名称，用于回写明细的项目名。
     */
    public static String name(int type) {
        return switch (type) {
            case DRUG -> "药品";
            case TREATMENT -> "诊疗";
            case EXAM -> "检查";
            case BED -> "床位";
            case NURSING -> "护理";
            case REGISTRATION -> "挂号";
            default -> "其他";
        };
    }
}
