package com.xunfang.healthcare.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.util.StrUtil;
import com.xunfang.healthcare.common.AppointmentStatus;
import com.xunfang.healthcare.common.BizException;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.entity.BizAppointment;
import com.xunfang.healthcare.entity.BizPatient;
import com.xunfang.healthcare.entity.BizRegistration;
import com.xunfang.healthcare.entity.BizSchedule;
import com.xunfang.healthcare.entity.SysDept;
import com.xunfang.healthcare.mapper.BizAppointmentMapper;
import com.xunfang.healthcare.mapper.BizPatientMapper;
import com.xunfang.healthcare.mapper.BizRegistrationMapper;
import com.xunfang.healthcare.mapper.BizScheduleMapper;
import com.xunfang.healthcare.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 线上线下预约挂号。
 */
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final BizAppointmentMapper bizAppointmentMapper;
    private final BizPatientMapper bizPatientMapper;
    private final BizScheduleMapper bizScheduleMapper;
    private final BizRegistrationMapper bizRegistrationMapper;
    private final SysDeptMapper sysDeptMapper;
    private final NotifyService notifyService;
    private final RegistrationService registrationService;

    /**
     * 预约分页。
     */
    public PageResult<BizAppointment> page(PageQuery query, Long deptId, Integer status) {
        Page<BizAppointment> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(bizAppointmentMapper.selectPage(page, Wrappers.<BizAppointment>lambdaQuery()
                .eq(deptId != null, BizAppointment::getDeptId, deptId)
                .eq(status != null, BizAppointment::getStatus, status)
                .ge(StrUtil.isNotBlank(query.getStartTime()),
                        BizAppointment::getAppointmentDate, query.getStartTime())
                .le(StrUtil.isNotBlank(query.getEndTime()),
                        BizAppointment::getAppointmentDate, query.getEndTime())
                .orderByDesc(BizAppointment::getAppointmentDate)));
    }

    /**
     * 新增预约：登记即占用对应排班的号源。
     * <p>
     * 状态一律从 0 待就诊开始，不接受调用方传入的状态；
     * 取消与爽约时再归还号源，保证「预约 + 挂号」不会超出总号源。
     * <p>
     * 同一患者在同一排班只允许存在一个在用号（预约 0/1 或挂号 0/1），
     * 否则预约完再来窗口挂号就会占两个号。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizAppointment create(BizAppointment appointment) {
        if (appointment.getPatientId() == null) {
            throw BizException.badRequest("患者不能为空");
        }
        if (appointment.getAppointmentDate() == null) {
            appointment.setAppointmentDate(LocalDate.now());
        }
        if (appointment.getAppointmentDate().isBefore(LocalDate.now())) {
            throw BizException.badRequest("预约日期不能早于今天");
        }
        BizSchedule schedule = resolveSchedule(appointment);
        requireNoDuplicateHold(appointment.getPatientId(), schedule.getId());
        occupySchedule(schedule, appointment.getAppointmentDate());
        // 号源定位到的排班要回写到预约上，取消/改约/爽约才有号可还
        appointment.setScheduleId(schedule.getId());
        appointment.setStatus(AppointmentStatus.PENDING);
        if (appointment.getSource() == null) {
            appointment.setSource(1);
        }
        bizAppointmentMapper.insert(appointment);
        notifyAppointmentCreated(appointment);
        return appointment;
    }

    /**
     * 同一患者在同一排班只能占一个号：已有的预约或已有的挂号都算。
     */
    private void requireNoDuplicateHold(Long patientId, Long scheduleId) {
        if (scheduleId == null) {
            return;
        }
        long appointments = bizAppointmentMapper.selectCount(Wrappers.<BizAppointment>lambdaQuery()
                .eq(BizAppointment::getPatientId, patientId)
                .eq(BizAppointment::getScheduleId, scheduleId)
                .in(BizAppointment::getStatus, AppointmentStatus.PENDING, AppointmentStatus.CHECKED_IN));
        if (appointments > 0) {
            throw BizException.badRequest("该患者在这个排班已经有预约了，请勿重复预约");
        }
        long registrations = bizRegistrationMapper.selectCount(Wrappers.<BizRegistration>lambdaQuery()
                .eq(BizRegistration::getPatientId, patientId)
                .eq(BizRegistration::getScheduleId, scheduleId)
                .in(BizRegistration::getStatus, 0, 1));
        if (registrations > 0) {
            throw BizException.badRequest("该患者在这个排班已经挂过号了，无需再预约");
        }
    }

    /**
     * 取号：把线上预约转成正式挂号记录，进入候诊队列。
     * <p>
     * 关键点是<b>不再扣号源</b>——预约成功时那一个号已经从总号里划走了，
     * 取号只是把它落成一条挂号记录并分配排队号。重复取号直接返回已有挂号（幂等）。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizRegistration checkin(Long id, Integer payType) {
        BizAppointment appointment = requireAppointment(id);
        int status = appointment.getStatus() == null ? AppointmentStatus.PENDING : appointment.getStatus();
        if (status == AppointmentStatus.CHECKED_IN) {
            BizRegistration existing = appointment.getRegistrationId() == null
                    ? null : bizRegistrationMapper.selectById(appointment.getRegistrationId());
            if (existing != null && existing.getStatus() != null && existing.getStatus() != 3) {
                return existing;
            }
            throw BizException.badRequest("该预约已取号，请到挂号记录中处理");
        }
        if (status != AppointmentStatus.PENDING) {
            throw BizException.badRequest(status == AppointmentStatus.CANCELLED
                    ? "该预约已取消，不能取号" : "该预约已标记爽约，不能取号");
        }
        if (appointment.getAppointmentDate() != null && appointment.getAppointmentDate().isAfter(LocalDate.now())) {
            throw BizException.badRequest("还没到预约日期（" + appointment.getAppointmentDate() + "），不能提前取号");
        }
        if (appointment.getScheduleId() == null) {
            throw BizException.badRequest("该预约没有关联号源，无法取号，请重新预约");
        }
        // 挂号服务内部会识别这条待取号预约，复用预约已占的号源
        return registrationService.register(appointment.getPatientId(), appointment.getScheduleId(),
                appointment.getRegType(), payType);
    }

    /**
     * 预约成功：短信提醒患者，给接诊医生发待办提醒。
     */
    private void notifyAppointmentCreated(BizAppointment appointment) {
        BizPatient patient = bizPatientMapper.selectById(appointment.getPatientId());
        if (patient == null) {
            return;
        }
        String deptName = deptName(appointment.getDeptId());
        String visitTime = appointment.getAppointmentDate() + " " + timePeriodName(appointment.getTimePeriod());
        notifyService.smsToPatient(patient, "APPOINTMENT_REMIND",
                patient.getName() + "您好，您已预约" + deptName + "门诊 " + visitTime + "，请提前15分钟到院取号。");
        notifyService.inboxToUser(appointment.getDoctorId(), "新预约提醒",
                "患者" + patient.getName() + "已预约" + deptName + visitTime + "，请按时接诊。",
                2, String.valueOf(appointment.getId()));
    }

    private String deptName(Long deptId) {
        if (deptId == null) {
            return "门诊";
        }
        SysDept dept = sysDeptMapper.selectById(deptId);
        return dept == null ? "门诊" : dept.getDeptName();
    }

    private String timePeriodName(Integer timePeriod) {
        if (timePeriod == null) {
            return "";
        }
        return switch (timePeriod) {
            case 1 -> "上午";
            case 2 -> "下午";
            case 3 -> "晚上";
            default -> "";
        };
    }

    /**
     * 取消预约：仅待就诊可取消，取消后归还号源。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, String reason) {
        BizAppointment appointment = requireAppointment(id);
        int status = appointment.getStatus() == null ? AppointmentStatus.PENDING : appointment.getStatus();
        if (status == AppointmentStatus.CHECKED_IN) {
            throw BizException.badRequest("该预约已取号并生成挂号记录，请到挂号管理中退号（退号后预约会自动退回待取号）");
        }
        if (status != AppointmentStatus.PENDING) {
            throw BizException.badRequest("该预约已取消或已爽约，无需重复操作");
        }
        BizAppointment update = new BizAppointment();
        update.setId(id);
        update.setStatus(AppointmentStatus.CANCELLED);
        update.setCancelReason(reason);
        bizAppointmentMapper.updateById(update);
        releaseSchedule(appointment.getScheduleId());
    }

    /**
     * 爽约：到点未就诊，标记爽约并归还号源。
     */
    @Transactional(rollbackFor = Exception.class)
    public void noShow(Long id) {
        BizAppointment appointment = requireAppointment(id);
        int status = appointment.getStatus() == null ? AppointmentStatus.PENDING : appointment.getStatus();
        if (status != AppointmentStatus.PENDING) {
            throw BizException.badRequest(status == AppointmentStatus.CHECKED_IN
                    ? "该预约已取号，如患者未就诊请在挂号管理中退号"
                    : "只有待取号的预约可以标记爽约");
        }
        BizAppointment update = new BizAppointment();
        update.setId(id);
        update.setStatus(AppointmentStatus.NO_SHOW);
        bizAppointmentMapper.updateById(update);
        releaseSchedule(appointment.getScheduleId());
    }

    /**
     * 改约：更换排班与日期。先占新号再还旧号，占号失败则整体回滚。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizAppointment reschedule(Long id, Long scheduleId, LocalDate appointmentDate) {
        BizAppointment appointment = requireAppointment(id);
        int status = appointment.getStatus() == null ? AppointmentStatus.PENDING : appointment.getStatus();
        if (status != AppointmentStatus.PENDING) {
            throw BizException.badRequest("只有待就诊的预约可以改约");
        }
        LocalDate targetDate = appointmentDate == null ? appointment.getAppointmentDate() : appointmentDate;
        if (targetDate == null || targetDate.isBefore(LocalDate.now())) {
            throw BizException.badRequest("改约日期不能早于今天");
        }

        BizAppointment update = new BizAppointment();
        update.setId(id);
        update.setAppointmentDate(targetDate);

        boolean sameSchedule = Objects.equals(scheduleId, appointment.getScheduleId());
        if (!sameSchedule) {
            BizAppointment probe = new BizAppointment();
            probe.setScheduleId(scheduleId);
            probe.setDoctorId(appointment.getDoctorId());
            probe.setAppointmentDate(targetDate);
            probe.setTimePeriod(appointment.getTimePeriod());
            BizSchedule schedule = resolveSchedule(probe);
            occupySchedule(schedule, targetDate);
            update.setScheduleId(schedule.getId());
            releaseSchedule(appointment.getScheduleId());
        } else if (scheduleId != null) {
            BizSchedule schedule = bizScheduleMapper.selectById(scheduleId);
            if (schedule != null && schedule.getScheduleDate() != null
                    && !schedule.getScheduleDate().equals(targetDate)) {
                throw BizException.badRequest("改约日期与所选排班日期不一致");
            }
        }
        update.setStatus(AppointmentStatus.PENDING);
        bizAppointmentMapper.updateById(update);
        return bizAppointmentMapper.selectById(id);
    }

    /**
     * 定位预约对应的排班：优先用显式传入的排班，否则按医生 + 日期 + 时段匹配。
     */
    private BizSchedule resolveSchedule(BizAppointment appointment) {
        if (appointment.getScheduleId() != null) {
            BizSchedule schedule = bizScheduleMapper.selectById(appointment.getScheduleId());
            if (schedule == null) {
                throw BizException.badRequest("排班不存在");
            }
            if (schedule.getScheduleDate() != null && appointment.getAppointmentDate() != null
                    && !schedule.getScheduleDate().equals(appointment.getAppointmentDate())) {
                throw BizException.badRequest("预约日期与排班日期不一致");
            }
            return schedule;
        }
        if (appointment.getDoctorId() == null) {
            throw BizException.badRequest("预约需先选择医生，以便占用对应号源");
        }
        BizSchedule schedule = bizScheduleMapper.selectOne(Wrappers.<BizSchedule>lambdaQuery()
                .eq(BizSchedule::getDoctorId, appointment.getDoctorId())
                .eq(appointment.getAppointmentDate() != null, BizSchedule::getScheduleDate,
                        appointment.getAppointmentDate())
                .eq(appointment.getTimePeriod() != null, BizSchedule::getTimePeriod, appointment.getTimePeriod())
                .eq(BizSchedule::getStatus, 1)
                .last("limit 1"));
        if (schedule == null) {
            throw BizException.badRequest("该医生在所选日期没有可预约的排班，请先安排排班");
        }
        return schedule;
    }

    /**
     * 占用一个号源，号源不足或排班停诊直接拒绝。
     */
    private void occupySchedule(BizSchedule schedule, LocalDate appointmentDate) {
        if (schedule == null) {
            return;
        }
        if (schedule.getStatus() != null && schedule.getStatus() == 0) {
            throw BizException.badRequest("该排班已停诊，无法预约");
        }
        if (appointmentDate != null && schedule.getScheduleDate() != null
                && !schedule.getScheduleDate().equals(appointmentDate)) {
            throw BizException.badRequest("预约日期与排班日期不一致");
        }
        int remain = schedule.getRemainNum() == null ? 0 : schedule.getRemainNum();
        if (remain <= 0) {
            throw BizException.badRequest("该排班号源已用完");
        }
        BizSchedule update = new BizSchedule();
        update.setId(schedule.getId());
        update.setRemainNum(remain - 1);
        bizScheduleMapper.updateById(update);
    }

    /**
     * 归还一个号源，且不超过总号源。
     */
    private void releaseSchedule(Long scheduleId) {
        if (scheduleId == null) {
            return;
        }
        BizSchedule schedule = bizScheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            return;
        }
        int remain = (schedule.getRemainNum() == null ? 0 : schedule.getRemainNum()) + 1;
        if (schedule.getTotalNum() != null && remain > schedule.getTotalNum()) {
            remain = schedule.getTotalNum();
        }
        BizSchedule update = new BizSchedule();
        update.setId(scheduleId);
        update.setRemainNum(remain);
        bizScheduleMapper.updateById(update);
    }

    private BizAppointment requireAppointment(Long id) {
        BizAppointment appointment = bizAppointmentMapper.selectById(id);
        if (appointment == null) {
            throw BizException.badRequest("预约不存在");
        }
        return appointment;
    }
}
