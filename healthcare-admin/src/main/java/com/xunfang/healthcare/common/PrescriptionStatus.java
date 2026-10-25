package com.xunfang.healthcare.common;

/**
 * 处方状态机（与 biz_prescription.status 注释一致）。
 * <p>
 * 处方就是医生自己开的医嘱，开方即生效，**不存在"审核"环节**。
 * 主链路：待收费 0 → 收费 2 → 发药 3；退药/退费 →4，收费前撤销 →5。
 * 每一档只能由对应业务节点推进，不允许跨档。
 */
public final class PrescriptionStatus {

    /**
     * 待收费：医生开方后的初始状态，收费台可直接检索并收费，但还不能发药。
     */
    public static final int PENDING_CHARGE = 0;

    /**
     * 历史「已审核」：旧版本走的是 0 待审核 → 1 已审核 → 2 已收费。
     * <p>
     * 审核环节已废弃，这个状态不再产生；残留数据在语义上等同于「待收费」，
     * 收费、作废时按未收费处理，不要当成另一档业务状态。
     */
    public static final int LEGACY_AUDITED = 1;

    /**
     * 已收费：收费完成，药房可以发药。
     */
    public static final int PAID = 2;

    /**
     * 已发药：药房已出库，不能再收费。
     */
    public static final int DISPENSED = 3;

    /**
     * 已退：退药或退费后回收状态。
     */
    public static final int REFUNDED = 4;

    /**
     * 已作废：医生在收费前撤销处方（开错药、患者改方案）。
     * <p>
     * 与 4「已退」区分开：作废从未产生过费用，退是收过费又退回。
     * 作废件对收费台、药房都不可见，只能查询留痕。
     * <p>
     * 早期实现把作废写成 3（已发药），会让药房把作废处方当成待退药单据，故单列一档。
     */
    public static final int CANCELLED = 5;

    /**
     * 是否属于「尚未收费」——待收费，或历史遗留的已审核。
     */
    public static boolean isUnpaid(Integer status) {
        return status == null || status == PENDING_CHARGE || status == LEGACY_AUDITED;
    }

    private PrescriptionStatus() {
    }
}
