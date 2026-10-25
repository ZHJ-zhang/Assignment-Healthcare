package com.xunfang.healthcare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xunfang.healthcare.entity.BizCharge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface BizChargeMapper extends BaseMapper<BizCharge> {

    /**
     * 按日汇总已支付收费单。
     */
    @Select("""
            SELECT DATE_FORMAT(charge_time, '%Y-%m-%d') AS stat_date,
                   COUNT(*) AS total_count,
                   IFNULL(SUM(actual_amount), 0) AS total_amount
            FROM biz_charge
            WHERE pay_status = 1 AND deleted = 0
              AND charge_time BETWEEN #{start} AND #{end}
            GROUP BY DATE_FORMAT(charge_time, '%Y-%m-%d')
            ORDER BY stat_date
            """)
    List<Map<String, Object>> selectDailySummary(@Param("start") String start, @Param("end") String end);

    /**
     * 按支付方式汇总已支付收费单。
     */
    @Select("""
            SELECT pay_type, COUNT(*) AS total_count, IFNULL(SUM(actual_amount), 0) AS total_amount
            FROM biz_charge
            WHERE pay_status = 1 AND deleted = 0
              AND charge_time BETWEEN #{start} AND #{end}
            GROUP BY pay_type
            """)
    List<Map<String, Object>> selectPayTypeSummary(@Param("start") String start, @Param("end") String end);

    /**
     * 按科室汇总已支付收费单。
     */
    @Select("""
            SELECT COALESCE(d.dept_name, '未分配') AS dept_name,
                   COUNT(*) AS total_count,
                   IFNULL(SUM(c.actual_amount), 0) AS total_amount
            FROM biz_charge c
            LEFT JOIN sys_dept d ON d.id = c.dept_id
            WHERE c.pay_status = 1 AND c.deleted = 0
              AND c.charge_time BETWEEN #{start} AND #{end}
            GROUP BY COALESCE(d.dept_name, '未分配')
            ORDER BY total_amount DESC
            """)
    List<Map<String, Object>> selectDeptSummary(@Param("start") String start, @Param("end") String end);
}
