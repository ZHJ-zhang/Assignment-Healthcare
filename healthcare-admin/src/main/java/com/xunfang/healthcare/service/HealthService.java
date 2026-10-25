package com.xunfang.healthcare.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xunfang.healthcare.common.BizException;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.entity.BizHealthRecord;
import com.xunfang.healthcare.entity.BizInfectiousReport;
import com.xunfang.healthcare.entity.BizPhysicalExam;
import com.xunfang.healthcare.entity.BizVaccination;
import com.xunfang.healthcare.mapper.BizHealthRecordMapper;
import com.xunfang.healthcare.mapper.BizInfectiousReportMapper;
import com.xunfang.healthcare.mapper.BizPhysicalExamMapper;
import com.xunfang.healthcare.mapper.BizVaccinationMapper;
import com.xunfang.healthcare.security.SecurityUtils;
import com.xunfang.healthcare.util.BizNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 公共卫生：居民健康档案、预防接种、传染病上报与健康体检。
 */
@Service
@RequiredArgsConstructor
public class HealthService {

    /**
     * 接种提醒提前天数。
     */
    private static final int REMIND_DAYS = 30;

    private final BizHealthRecordMapper healthRecordMapper;
    private final BizVaccinationMapper vaccinationMapper;
    private final BizInfectiousReportMapper infectiousReportMapper;
    private final BizPhysicalExamMapper physicalExamMapper;
    private final BizNoGenerator bizNoGenerator;

    /**
     * 健康档案分页。
     */
    public PageResult<BizHealthRecord> pageRecords(PageQuery query, String community, Integer recordType) {
        Page<BizHealthRecord> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(healthRecordMapper.selectPage(page, Wrappers.<BizHealthRecord>lambdaQuery()
                .like(StrUtil.isNotBlank(query.getKeyword()), BizHealthRecord::getName, query.getKeyword())
                .eq(StrUtil.isNotBlank(community), BizHealthRecord::getCommunity, community)
                .eq(recordType != null, BizHealthRecord::getRecordType, recordType)
                .orderByDesc(BizHealthRecord::getCreateTime)));
    }

    /**
     * 保存健康档案。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizHealthRecord saveRecord(BizHealthRecord record) {
        if (record.getId() == null) {
            if (StrUtil.isBlank(record.getRecordNo())) {
                record.setRecordNo(bizNoGenerator.next("HR"));
            }
            if (record.getCreateDate() == null) {
                record.setCreateDate(LocalDate.now());
            }
            healthRecordMapper.insert(record);
        } else {
            healthRecordMapper.updateById(record);
        }
        return record;
    }

    /**
     * 删除健康档案。
     */
    public void deleteRecord(Long id) {
        if (healthRecordMapper.selectById(id) == null) {
            throw BizException.badRequest("健康档案不存在");
        }
        healthRecordMapper.deleteById(id);
    }

    /**
     * 预防接种分页。
     */
    public PageResult<BizVaccination> pageVaccinations(PageQuery query, Long patientId, Integer status) {
        Page<BizVaccination> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(vaccinationMapper.selectPage(page, Wrappers.<BizVaccination>lambdaQuery()
                .eq(patientId != null, BizVaccination::getPatientId, patientId)
                .eq(status != null, BizVaccination::getStatus, status)
                .like(StrUtil.isNotBlank(query.getKeyword()), BizVaccination::getName, query.getKeyword())
                .orderByDesc(BizVaccination::getVaccinationDate)));
    }

    /**
     * 登记预防接种。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizVaccination saveVaccination(BizVaccination vaccination) {
        if (vaccination.getId() == null) {
            if (StrUtil.isBlank(vaccination.getVaccinationNo())) {
                vaccination.setVaccinationNo(bizNoGenerator.next("VAC"));
            }
            if (vaccination.getStatus() == null) {
                vaccination.setStatus(1);
            }
            vaccination.setDoctorId(SecurityUtils.getUserIdOrNull());
            vaccinationMapper.insert(vaccination);
        } else {
            vaccinationMapper.updateById(vaccination);
        }
        return vaccination;
    }

    /**
     * 接种提醒列表：下次接种日期在近期内的记录。
     */
    public PageResult<BizVaccination> vaccinationRemind(PageQuery query) {
        Page<BizVaccination> page = new Page<>(query.getPageNum(), query.getPageSize());
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(REMIND_DAYS);
        return PageResult.of(vaccinationMapper.selectPage(page, Wrappers.<BizVaccination>lambdaQuery()
                .eq(BizVaccination::getStatus, 1)
                .isNotNull(BizVaccination::getNextDate)
                .ge(BizVaccination::getNextDate, today)
                .le(BizVaccination::getNextDate, deadline)
                .orderByAsc(BizVaccination::getNextDate)));
    }

    /**
     * 删除接种记录。
     */
    public void deleteVaccination(Long id) {
        if (vaccinationMapper.selectById(id) == null) {
            throw BizException.badRequest("接种记录不存在");
        }
        vaccinationMapper.deleteById(id);
    }

    /**
     * 传染病上报分页。
     */
    public PageResult<BizInfectiousReport> pageInfectious(PageQuery query, Integer status, String diseaseName) {
        Page<BizInfectiousReport> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(infectiousReportMapper.selectPage(page, Wrappers.<BizInfectiousReport>lambdaQuery()
                .eq(status != null, BizInfectiousReport::getStatus, status)
                .like(StrUtil.isNotBlank(diseaseName), BizInfectiousReport::getDiseaseName, diseaseName)
                .like(StrUtil.isNotBlank(query.getKeyword()), BizInfectiousReport::getName, query.getKeyword())
                .orderByDesc(BizInfectiousReport::getReportDate)));
    }

    /**
     * 传染病上报。
     * <p>
     * 新建一律落到「0 待上报」，不允许前端直接把状态写成已上报或已审核，
     * 否则审核环节形同虚设。已审核的报卡是终态，不再允许改内容。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizInfectiousReport saveInfectious(BizInfectiousReport report) {
        if (report.getId() == null) {
            if (StrUtil.isBlank(report.getReportNo())) {
                report.setReportNo(bizNoGenerator.next("ID"));
            }
            if (report.getReportDate() == null) {
                report.setReportDate(LocalDate.now());
            }
            report.setStatus(0);
            report.setAuditTime(null);
            report.setAuditRemark(null);
            infectiousReportMapper.insert(report);
        } else {
            BizInfectiousReport exists = requireInfectious(report.getId());
            if (exists.getStatus() != null && exists.getStatus() == 2) {
                throw BizException.badRequest("该报卡已审核，不能再修改");
            }
            // 状态只能通过提交/审核入口流转，改内容时忽略前端传来的状态
            report.setStatus(null);
            infectiousReportMapper.updateById(report);
        }
        return infectiousReportMapper.selectById(report.getId());
    }

    /**
     * 提交上报：0 待上报 → 1 已上报。
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitInfectious(Long id) {
        BizInfectiousReport report = requireInfectious(id);
        int status = report.getStatus() == null ? 0 : report.getStatus();
        if (status == 2) {
            throw BizException.badRequest("该报卡已审核，无需重复上报");
        }
        if (status == 1) {
            throw BizException.badRequest("该报卡已上报，请勿重复提交");
        }
        infectiousReportMapper.update(null, Wrappers.<BizInfectiousReport>lambdaUpdate()
                .eq(BizInfectiousReport::getId, id)
                .set(BizInfectiousReport::getStatus, 1)
                .set(BizInfectiousReport::getReportDate, report.getReportDate() == null
                        ? LocalDate.now() : report.getReportDate()));
    }

    /**
     * 审核报卡：1 已上报 → 2 已审核。
     * <p>
     * 待上报的草稿不能越过上报直接审核，否则上报时限统计会失真。
     */
    @Transactional(rollbackFor = Exception.class)
    public void auditInfectious(Long id, String remark) {
        BizInfectiousReport report = requireInfectious(id);
        int status = report.getStatus() == null ? 0 : report.getStatus();
        if (status == 2) {
            throw BizException.badRequest("该报卡已审核，无需重复操作");
        }
        if (status == 0) {
            throw BizException.badRequest("该报卡尚未上报，请先提交上报再审核");
        }
        infectiousReportMapper.update(null, Wrappers.<BizInfectiousReport>lambdaUpdate()
                .eq(BizInfectiousReport::getId, id)
                .set(BizInfectiousReport::getStatus, 2)
                .set(BizInfectiousReport::getAuditTime, LocalDateTime.now())
                .set(BizInfectiousReport::getAuditRemark, StrUtil.blankToDefault(remark, "审核通过")));
    }

    private BizInfectiousReport requireInfectious(Long id) {
        BizInfectiousReport report = infectiousReportMapper.selectById(id);
        if (report == null) {
            throw BizException.badRequest("传染病报卡不存在");
        }
        return report;
    }

    /**
     * 健康体检分页。
     */
    public PageResult<BizPhysicalExam> pagePhysicalExams(PageQuery query, Long patientId, Integer status) {
        Page<BizPhysicalExam> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(physicalExamMapper.selectPage(page, Wrappers.<BizPhysicalExam>lambdaQuery()
                .eq(patientId != null, BizPhysicalExam::getPatientId, patientId)
                .eq(status != null, BizPhysicalExam::getStatus, status)
                .like(StrUtil.isNotBlank(query.getKeyword()), BizPhysicalExam::getName, query.getKeyword())
                .orderByDesc(BizPhysicalExam::getExamDate)));
    }

    /**
     * 录入体检结果。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizPhysicalExam savePhysicalExam(BizPhysicalExam exam) {
        if (exam.getId() == null) {
            if (StrUtil.isBlank(exam.getExamNo())) {
                exam.setExamNo(bizNoGenerator.next("PE"));
            }
            if (exam.getExamDate() == null) {
                exam.setExamDate(LocalDate.now());
            }
            if (exam.getStatus() == null) {
                exam.setStatus(1);
            }
            physicalExamMapper.insert(exam);
        } else {
            physicalExamMapper.updateById(exam);
        }
        return exam;
    }

    /**
     * 删除体检记录。
     */
    public void deletePhysicalExam(Long id) {
        if (physicalExamMapper.selectById(id) == null) {
            throw BizException.badRequest("体检记录不存在");
        }
        physicalExamMapper.deleteById(id);
    }
}
