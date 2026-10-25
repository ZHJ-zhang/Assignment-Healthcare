package com.xunfang.healthcare.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 统计报表专用查询。
 */
@Mapper
public interface ReportMapper {

    /**
     * 门诊量按日汇总。
     */
    @Select("""
            SELECT DATE_FORMAT(reg_time, '%Y-%m-%d') AS stat_date,
                   COUNT(*) AS total_count,
                   SUM(CASE WHEN reg_type = 2 THEN 1 ELSE 0 END) AS expert_count
            FROM biz_registration
            WHERE deleted = 0 AND reg_time BETWEEN #{start} AND #{end}
            GROUP BY DATE_FORMAT(reg_time, '%Y-%m-%d')
            ORDER BY stat_date
            """)
    List<Map<String, Object>> selectOutpatientDaily(@Param("start") String start, @Param("end") String end);

    /**
     * 门诊量按科室汇总。
     */
    @Select("""
            SELECT COALESCE(d.dept_name, '未分配') AS dept_name,
                   COUNT(*) AS total_count
            FROM biz_registration r
            LEFT JOIN sys_dept d ON d.id = r.dept_id
            WHERE r.deleted = 0 AND r.reg_time BETWEEN #{start} AND #{end}
            GROUP BY COALESCE(d.dept_name, '未分配')
            ORDER BY total_count DESC
            """)
    List<Map<String, Object>> selectOutpatientByDept(@Param("start") String start, @Param("end") String end);

    /**
     * 门诊量按医生汇总。
     */
    @Select("""
            SELECT COALESCE(u.real_name, u.username, '未知') AS doctor_name,
                   COUNT(*) AS total_count
            FROM biz_registration r
            LEFT JOIN sys_user u ON u.id = r.doctor_id
            WHERE r.deleted = 0 AND r.reg_time BETWEEN #{start} AND #{end}
            GROUP BY COALESCE(u.real_name, u.username, '未知')
            ORDER BY total_count DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> selectOutpatientByDoctor(@Param("start") String start, @Param("end") String end,
                                                        @Param("limit") int limit);

    /**
     * 住院入出院按日汇总。
     */
    @Select("""
            SELECT DATE_FORMAT(admission_date, '%Y-%m-%d') AS stat_date,
                   COUNT(*) AS admission_count,
                   SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS discharge_count
            FROM biz_admission
            WHERE deleted = 0 AND admission_date BETWEEN #{start} AND #{end}
            GROUP BY DATE_FORMAT(admission_date, '%Y-%m-%d')
            ORDER BY stat_date
            """)
    List<Map<String, Object>> selectInpatientDaily(@Param("start") String start, @Param("end") String end);

    /**
     * 在院人数按科室汇总。
     */
    @Select("""
            SELECT COALESCE(d.dept_name, '未分配') AS dept_name,
                   COUNT(*) AS in_hospital_count
            FROM biz_admission a
            LEFT JOIN sys_dept d ON d.id = a.dept_id
            WHERE a.deleted = 0 AND a.status = 0
            GROUP BY COALESCE(d.dept_name, '未分配')
            ORDER BY in_hospital_count DESC
            """)
    List<Map<String, Object>> selectInpatientByDept();

    /**
     * 药品销量排行。
     */
    @Select("""
            SELECT COALESCE(i.drug_name, '未知') AS drug_name,
                   IFNULL(SUM(i.quantity), 0) AS total_quantity,
                   IFNULL(SUM(i.amount), 0) AS total_amount
            FROM biz_prescription_item i
            WHERE i.deleted = 0 AND i.create_time BETWEEN #{start} AND #{end}
            GROUP BY COALESCE(i.drug_name, '未知')
            ORDER BY total_quantity DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> selectDrugRanking(@Param("start") String start, @Param("end") String end,
                                                 @Param("limit") int limit);

    /**
     * 新增患者按日汇总。
     */
    @Select("""
            SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS stat_date,
                   COUNT(*) AS total_count
            FROM biz_patient
            WHERE deleted = 0 AND create_time BETWEEN #{start} AND #{end}
            GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')
            ORDER BY stat_date
            """)
    List<Map<String, Object>> selectPatientDaily(@Param("start") String start, @Param("end") String end);

    /**
     * 患者性别分布。
     */
    @Select("""
            SELECT CASE gender WHEN 1 THEN '男' WHEN 0 THEN '女' ELSE '未知' END AS gender_name,
                   COUNT(*) AS total_count
            FROM biz_patient
            WHERE deleted = 0
            GROUP BY CASE gender WHEN 1 THEN '男' WHEN 0 THEN '女' ELSE '未知' END
            """)
    List<Map<String, Object>> selectPatientByGender();

    /**
     * 患者年龄段分布。
     */
    @Select("""
            SELECT CASE
                     WHEN TIMESTAMPDIFF(YEAR, birth_date, CURDATE()) < 18 THEN '0-17岁'
                     WHEN TIMESTAMPDIFF(YEAR, birth_date, CURDATE()) < 35 THEN '18-34岁'
                     WHEN TIMESTAMPDIFF(YEAR, birth_date, CURDATE()) < 60 THEN '35-59岁'
                     ELSE '60岁以上'
                   END AS age_range,
                   COUNT(*) AS total_count
            FROM biz_patient
            WHERE deleted = 0 AND birth_date IS NOT NULL
            GROUP BY age_range
            ORDER BY MIN(birth_date) DESC
            """)
    List<Map<String, Object>> selectPatientByAgeRange();

    /**
     * 就诊人次按科室汇总（门诊 + 住院）。
     */
    @Select("""
            SELECT COALESCE(d.dept_name, '未分配') AS dept_name,
                   COUNT(*) AS visit_count
            FROM biz_medical_record m
            LEFT JOIN sys_dept d ON d.id = m.dept_id
            WHERE m.deleted = 0 AND m.visit_time BETWEEN #{start} AND #{end}
            GROUP BY COALESCE(d.dept_name, '未分配')
            ORDER BY visit_count DESC
            """)
    List<Map<String, Object>> selectVisitByDept(@Param("start") String start, @Param("end") String end);
}
