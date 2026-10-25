package com.xunfang.healthcare.common;

/**
 * 预约状态机（与 biz_appointment.status 注释一致）。
 * <p>
 * 线上预约与线下挂号共用同一个号源池：预约成功即占号（remain_num - 1），
 * 到院「取号」把预约转成正式挂号记录，<b>不再重复扣号</b>。
 * <p>
 * 主链路：预约成功 0待就诊 → 取号转挂号 1已取号，取消 2已取消，未到院 3爽约。
 * 只有 0 占用号源；2 取消、3 爽约、以及退号（1 回退到 0 再退）都必须归还号源。
 */
public final class AppointmentStatus {

    /**
     * 待就诊（已预约待取号）：预约成功即为此状态，占用排班号源。
     */
    public static final int PENDING = 0;

    /**
     * 已取号：已转为正式挂号记录，号源就是预约时占的那一个，不再二次扣减。
     */
    public static final int VISITED = 1;

    /**
     * 已取号，与 {@link #VISITED} 同值，语义更贴近业务。
     */
    public static final int CHECKED_IN = VISITED;

    /**
     * 已取消：患者主动取消，归还号源。
     */
    public static final int CANCELLED = 2;

    /**
     * 爽约：到点未就诊，归还号源。
     */
    public static final int NO_SHOW = 3;

    private AppointmentStatus() {
    }
}
