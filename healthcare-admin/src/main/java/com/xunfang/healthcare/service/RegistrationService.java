package com.xunfang.healthcare.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.util.StrUtil;
import com.xunfang.healthcare.common.AppointmentStatus;
import com.xunfang.healthcare.common.BizException;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.entity.BizAppointment;
import com.xunfang.healthcare.entity.BizCallRecord;
import com.xunfang.healthcare.entity.BizExamApply;
import com.xunfang.healthcare.entity.BizPatient;
import com.xunfang.healthcare.entity.BizRegistration;
import com.xunfang.healthcare.entity.BizSchedule;
import com.xunfang.healthcare.mapper.BizAppointmentMapper;
import com.xunfang.healthcare.mapper.BizCallRecordMapper;
import com.xunfang.healthcare.mapper.BizExamApplyMapper;
import com.xunfang.healthcare.mapper.BizPatientMapper;
import com.xunfang.healthcare.mapper.BizRegistrationMapper;
import com.xunfang.healthcare.mapper.BizScheduleMapper;
import com.xunfang.healthcare.security.DataScopeHelper;
import com.xunfang.healthcare.util.BizNoGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * 排班、挂号、候诊队列与叫号。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final String QUEUE_KEY_PREFIX = "healthcare:queue:";

    private final BizScheduleMapper bizScheduleMapper;
    private final BizRegistrationMapper bizRegistrationMapper;
    private final BizAppointmentMapper bizAppointmentMapper;
    private final BizCallRecordMapper bizCallRecordMapper;
    private final DataScopeHelper dataScopeHelper;
    private final BizExamApplyMapper bizExamApplyMapper;
    private final BizPatientMapper bizPatientMapper;
    private final BizNoGenerator bizNoGenerator;
    private final StringRedisTemplate stringRedisTemplate;
    private final ChargeService chargeService;

    /**
     * 排班查询。
     */
    public List<BizSchedule> listSchedules(LocalDate date, Long deptId, Long doctorId) {
        return bizScheduleMapper.selectList(Wrappers.<BizSchedule>lambdaQuery()
                .eq(date != null, BizSchedule::getScheduleDate, date)
                .eq(deptId != null, BizSchedule::getDeptId, deptId)
                .eq(doctorId != null, BizSchedule::getDoctorId, doctorId)
                .orderByAsc(BizSchedule::getScheduleDate)
                .orderByAsc(BizSchedule::getTimePeriod));
    }

    /**
     * 保存排班。
     */
    public BizSchedule saveSchedule(BizSchedule schedule) {
        if (schedule.getId() == null) {
            if (schedule.getStatus() == null) {
                schedule.setStatus(1);
            }
            if (schedule.getTotalNum() != null && schedule.getRemainNum() == null) {
                schedule.setRemainNum(schedule.getTotalNum());
            }
            if (schedule.getRegFee() == null) {
                schedule.setRegFee(BigDecimal.ZERO);
            }
            bizScheduleMapper.insert(schedule);
        } else {
            bizScheduleMapper.updateById(schedule);
        }
        return schedule;
    }

    /**
     * 停诊。
     */
    public void stopSchedule(Long id) {
        BizSchedule schedule = requireSchedule(id);
        BizSchedule update = new BizSchedule();
        update.setId(schedule.getId());
        update.setStatus(0);
        bizScheduleMapper.updateById(update);
    }

    /**
     * 挂号（线下窗口挂号 / 线上预约取号共用）。
     * <p>
     * 号源只有一个池子：线上预约成功时就已经扣了一个号，到院取号只是把预约转成挂号记录，
     * <b>不能再扣一次</b>，否则同一个患者预约 + 挂号会吃掉两个号。
     * <p>
     * 因此这里先找该患者在该排班上「待取号」的预约：
     * 找到 → 复用预约占的号，不扣号源，并回写预约的 registrationId；
     * 找不到 → 走普通挂号，正常扣号。
     * <p>
     * 挂号费两种情况都要收：预约阶段不收钱，钱是在取号/挂号这一刻交的。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizRegistration register(Long patientId, Long scheduleId, Integer regType, Integer payType) {
        BizPatient patient = bizPatientMapper.selectById(patientId);
        if (patient == null) {
            throw BizException.badRequest("患者不存在");
        }
        BizSchedule schedule = requireSchedule(scheduleId);
        if (schedule.getStatus() != null && schedule.getStatus() == 0) {
            throw BizException.badRequest("该排班已停诊");
        }

        // 同一患者在同一排班只能有一个在用（待就诊或就诊中）的号
        long holds = bizRegistrationMapper.selectCount(Wrappers.<BizRegistration>lambdaQuery()
                .eq(BizRegistration::getPatientId, patientId)
                .eq(BizRegistration::getScheduleId, scheduleId)
                .in(BizRegistration::getStatus, 0, 1));
        if (holds > 0) {
            throw BizException.badRequest("该患者在这个排班已经有号了，请勿重复挂号");
        }

        BizAppointment appointment = bizAppointmentMapper.selectOne(Wrappers.<BizAppointment>lambdaQuery()
                .eq(BizAppointment::getPatientId, patientId)
                .eq(BizAppointment::getScheduleId, scheduleId)
                .eq(BizAppointment::getStatus, AppointmentStatus.PENDING)
                .orderByAsc(BizAppointment::getCreateTime)
                .last("limit 1"));

        if (appointment == null) {
            if (schedule.getRemainNum() != null && schedule.getRemainNum() <= 0) {
                throw BizException.badRequest("该排班号源已用完");
            }
            BizSchedule scheduleUpdate = new BizSchedule();
            scheduleUpdate.setId(schedule.getId());
            scheduleUpdate.setRemainNum(schedule.getRemainNum() - 1);
            bizScheduleMapper.updateById(scheduleUpdate);
        }
        // 复用预约时不再扣号：这一个号在预约成功那一刻就已经从总号里划走了

        BizRegistration registration = new BizRegistration();
        registration.setRegNo(bizNoGenerator.next("REG"));
        registration.setPatientId(patientId);
        registration.setDoctorId(schedule.getDoctorId());
        registration.setDeptId(schedule.getDeptId());
        registration.setScheduleId(schedule.getId());
        registration.setRegType(resolveRegType(appointment, schedule, regType));
        registration.setRegFee(schedule.getRegFee());
        registration.setQueueNo(nextQueueNo(schedule.getDeptId()));
        registration.setStatus(0);
        registration.setRegTime(LocalDateTime.now());
        bizRegistrationMapper.insert(registration);

        // 预约转为「已取号」，并记下对应的挂号记录，退号时才能回退
        if (appointment != null) {
            BizAppointment apptUpdate = new BizAppointment();
            apptUpdate.setId(appointment.getId());
            apptUpdate.setStatus(AppointmentStatus.CHECKED_IN);
            apptUpdate.setRegistrationId(registration.getId());
            bizAppointmentMapper.updateById(apptUpdate);
        }

        chargeService.createRegistrationFeeCharge(registration, schedule.getRegFee(), payType);
        return registration;
    }

    /**
     * 号别优先取预约上填的，其次取调用方传入，最后落到排班默认值。
     */
    private Integer resolveRegType(BizAppointment appointment, BizSchedule schedule, Integer regType) {
        if (appointment != null && appointment.getRegType() != null) {
            return appointment.getRegType();
        }
        return regType == null ? schedule.getRegType() : regType;
    }

    /**
     * 退号：仅「待就诊」可退。
     * <p>
     * 就诊中的患者可能已经开了处方、做了收费，必须先作废处方或完成退费才能退号，
     * 否则会出现「号退了但药还在」的账实不符。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, String reason) {
        BizRegistration registration = requireRegistration(id);
        int status = registration.getStatus() == null ? 0 : registration.getStatus();
        if (status == 1) {
            throw BizException.badRequest("患者就诊中，不能退号；请先作废处方或完成退费");
        }
        if (status == 2) {
            throw BizException.badRequest("该挂号已完成，不能退号");
        }
        if (status == 3) {
            throw BizException.badRequest("该挂号已退号，无需重复操作");
        }
        long paidExam = bizExamApplyMapper.selectCount(Wrappers.<BizExamApply>lambdaQuery()
                .eq(BizExamApply::getVisitId, id)
                .ge(BizExamApply::getStatus, 1));
        if (paidExam > 0) {
            throw BizException.badRequest("该就诊已产生检查单，请先退掉检查费再退号");
        }
        BizRegistration update = new BizRegistration();
        update.setId(id);
        update.setStatus(3);
        update.setCancelReason(reason);
        bizRegistrationMapper.updateById(update);

        // 挂号费已经收过，退号必须同步退回，否则钱和号对不上
        chargeService.refundRegistrationFee(id);

        // 这一号若是预约取号来的，把预约退回「待取号」，患者可以重新取号
        BizAppointment source = bizAppointmentMapper.selectOne(Wrappers.<BizAppointment>lambdaQuery()
                .eq(BizAppointment::getRegistrationId, id)
                .eq(BizAppointment::getStatus, AppointmentStatus.CHECKED_IN)
                .last("limit 1"));
        if (source != null) {
            bizAppointmentMapper.update(null, Wrappers.<BizAppointment>lambdaUpdate()
                    .eq(BizAppointment::getId, source.getId())
                    .set(BizAppointment::getStatus, AppointmentStatus.PENDING)
                    .set(BizAppointment::getRegistrationId, null));
        }

        if (registration.getScheduleId() != null) {
            BizSchedule schedule = bizScheduleMapper.selectById(registration.getScheduleId());
            if (schedule != null) {
                int remain = (schedule.getRemainNum() == null ? 0 : schedule.getRemainNum()) + 1;
                if (schedule.getTotalNum() != null && remain > schedule.getTotalNum()) {
                    remain = schedule.getTotalNum();
                }
                BizSchedule scheduleUpdate = new BizSchedule();
                scheduleUpdate.setId(schedule.getId());
                scheduleUpdate.setRemainNum(remain);
                bizScheduleMapper.updateById(scheduleUpdate);
            }
        }
    }

    /**
     * 候诊队列。
     */
    public List<BizRegistration> queue(Long deptId) {
        return queue(deptId, null);
    }

    /**
     * 候诊队列，可按医生过滤：一个医生一个队列，排队号互不干扰。
     * <p>
     * 大屏是科室级的公屏，所以这里不做科室收窄；但医生的数据范围是"仅本人"，
     * 只能看到挂在自己名下的号——否则他能在屏上看到并叫到别的医生的患者。
     */
    public List<BizRegistration> queue(Long deptId, Long doctorId) {
        LambdaQueryWrapper<BizRegistration> wrapper = Wrappers.<BizRegistration>lambdaQuery()
                .eq(deptId != null, BizRegistration::getDeptId, deptId)
                .eq(doctorId != null, BizRegistration::getDoctorId, doctorId)
                .in(BizRegistration::getStatus, 0, 1)
                .orderByAsc(BizRegistration::getQueueNo);
        dataScopeHelper.applySelf(wrapper, BizRegistration::getDoctorId);
        return bizRegistrationMapper.selectList(wrapper);
    }

    /**
     * 叫号：只有当天、当前时段、且该医生手头没有正在就诊的患者时才能叫号。
     * <p>
     * 一个医生同一时间只能接诊一个人，因此「叫号」与「接诊」共用同一套并发校验，
     * 否则会出现同一医生名下多人同时处于就诊中。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizCallRecord call(Long registrationId) {
        BizRegistration registration = requireRegistration(registrationId);
        int status = registration.getStatus() == null ? 0 : registration.getStatus();
        if (status != 0) {
            throw BizException.badRequest(status == 1 ? "该患者已在就诊中" : "该挂号状态不可叫号");
        }
        requireCallableSchedule(registration);
        assertDoctorIdle(registration);

        BizRegistration update = new BizRegistration();
        update.setId(registrationId);
        update.setStatus(1);
        bizRegistrationMapper.updateById(update);

        BizCallRecord record = new BizCallRecord();
        record.setDeptId(registration.getDeptId());
        record.setRegistrationId(registrationId);
        record.setPatientId(registration.getPatientId());
        record.setQueueNo(registration.getQueueNo());
        record.setCallNo(nextCallNo(registration.getDeptId()));
        record.setCallTime(LocalDateTime.now());
        record.setStatus(1);
        bizCallRecordMapper.insert(record);
        return record;
    }

    /**
     * 过号：患者被叫到但没来。
     * <p>
     * 叫号记录置 3 已过号，同时把这个挂号退回 0 待就诊，
     * 让它重新回到候诊队列——过号不等于作废，患者回来还能再叫。
     */
    @Transactional(rollbackFor = Exception.class)
    public void passCall(Long registrationId) {
        BizRegistration registration = requireRegistration(registrationId);
        int status = registration.getStatus() == null ? 0 : registration.getStatus();
        if (status != 1) {
            throw BizException.badRequest(status == 0
                    ? "该患者还没叫号，无需做过号处理" : "该挂号当前状态不能过号");
        }
        BizCallRecord record = requireActiveCall(registrationId);

        BizCallRecord recordUpdate = new BizCallRecord();
        recordUpdate.setId(record.getId());
        recordUpdate.setStatus(3);
        bizCallRecordMapper.updateById(recordUpdate);

        BizRegistration regUpdate = new BizRegistration();
        regUpdate.setId(registrationId);
        regUpdate.setStatus(0);
        bizRegistrationMapper.updateById(regUpdate);
    }

    /**
     * 叫号后完成：患者已就诊结束。
     * <p>
     * 叫号记录置 2 已就诊，挂号同步置 2 已完成，两步一起做，
     * 避免出现「队列里显示已就诊、挂号还挂着就诊中」这种对不上的情况。
     */
    @Transactional(rollbackFor = Exception.class)
    public void finishCall(Long registrationId) {
        BizRegistration registration = requireRegistration(registrationId);
        int status = registration.getStatus() == null ? 0 : registration.getStatus();
        if (status == 2) {
            throw BizException.badRequest("该挂号已完成就诊");
        }
        if (status != 1) {
            throw BizException.badRequest(status == 0
                    ? "该患者还没叫号，不能完成就诊" : "该挂号已退号，不能完成就诊");
        }
        BizCallRecord record = bizCallRecordMapper.selectOne(Wrappers.<BizCallRecord>lambdaQuery()
                .eq(BizCallRecord::getRegistrationId, registrationId)
                .eq(BizCallRecord::getStatus, 1)
                .orderByDesc(BizCallRecord::getCallTime)
                .last("limit 1"));
        if (record != null) {
            BizCallRecord recordUpdate = new BizCallRecord();
            recordUpdate.setId(record.getId());
            recordUpdate.setStatus(2);
            bizCallRecordMapper.updateById(recordUpdate);
        }

        BizRegistration regUpdate = new BizRegistration();
        regUpdate.setId(registrationId);
        regUpdate.setStatus(2);
        bizRegistrationMapper.updateById(regUpdate);
    }

    /**
     * 取该挂号当前处于「已叫号」的那条叫号记录。
     */
    private BizCallRecord requireActiveCall(Long registrationId) {
        BizCallRecord record = bizCallRecordMapper.selectOne(Wrappers.<BizCallRecord>lambdaQuery()
                .eq(BizCallRecord::getRegistrationId, registrationId)
                .eq(BizCallRecord::getStatus, 1)
                .orderByDesc(BizCallRecord::getCallTime)
                .last("limit 1"));
        if (record == null) {
            throw BizException.badRequest("没有找到该挂号的有效叫号记录");
        }
        return record;
    }

    /**
     * 校验号源是否属于今天、且时段没有错位。
     * <p>
     * 上午号只能在 12 点前叫，下午与夜间号 12 点后才可叫，
     * 因此上午和下午的号不会在同一时刻都能叫，也不会把当天所有号一次性锁死。
     */
    private void requireCallableSchedule(BizRegistration registration) {
        if (registration.getScheduleId() == null) {
            return;
        }
        BizSchedule schedule = bizScheduleMapper.selectById(registration.getScheduleId());
        if (schedule == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        if (schedule.getScheduleDate() != null && !today.equals(schedule.getScheduleDate())) {
            throw BizException.badRequest("该号源属于 " + schedule.getScheduleDate() + " 的排班，不是今天的号，不能叫号");
        }
        Integer period = schedule.getTimePeriod();
        if (period == null) {
            return;
        }
        boolean morning = LocalTime.now().getHour() < 12;
        if (period == 1 && !morning) {
            throw BizException.badRequest("该号属于上午时段，当前已过上午，不能叫号");
        }
        if (period != 1 && morning) {
            throw BizException.badRequest("该号属于" + periodName(period) + "时段，尚未到就诊时间，不能叫号");
        }
    }

    /**
     * 同一医生同时只允许一位患者处于就诊中。
     */
    private void assertDoctorIdle(BizRegistration registration) {
        if (registration.getDoctorId() == null) {
            return;
        }
        long busy = bizRegistrationMapper.selectCount(Wrappers.<BizRegistration>lambdaQuery()
                .eq(BizRegistration::getDoctorId, registration.getDoctorId())
                .eq(BizRegistration::getStatus, 1));
        if (busy > 0) {
            throw BizException.badRequest("该医生当前有患者正在就诊，请先完成当前就诊再接诊下一位");
        }
    }

    private String periodName(Integer period) {
        if (period == null) {
            return "";
        }
        return switch (period) {
            case 1 -> "上午";
            case 2 -> "下午";
            case 3 -> "夜间";
            default -> "";
        };
    }

    /**
     * 挂号记录分页。
     */
    public PageResult<BizRegistration> pageRegistrations(PageQuery query, Long deptId,
                                                          Integer status, Long doctorId) {
        return pageRegistrations(query, deptId, status, doctorId, null);
    }

    /**
     * 挂号分页，可按患者过滤。
     * <p>
     * 收费台选患者时需要拿"这次就诊属于哪个科室"，就是靠 patientId 查最近一条挂号。
     */
    public PageResult<BizRegistration> pageRegistrations(PageQuery query, Long deptId,
                                                          Integer status, Long doctorId, Long patientId) {
        Page<BizRegistration> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(bizRegistrationMapper.selectPage(page, Wrappers.<BizRegistration>lambdaQuery()
                .eq(deptId != null, BizRegistration::getDeptId, deptId)
                .eq(doctorId != null, BizRegistration::getDoctorId, doctorId)
                .eq(patientId != null, BizRegistration::getPatientId, patientId)
                .eq(status != null, BizRegistration::getStatus, status)
                .ge(StrUtil.isNotBlank(query.getStartTime()),
                        BizRegistration::getRegTime, query.getStartTime())
                .le(StrUtil.isNotBlank(query.getEndTime()),
                        BizRegistration::getRegTime, query.getEndTime() + " 23:59:59")
                .orderByDesc(BizRegistration::getRegTime)));
    }

    /**
     * 接诊：将挂号状态由「待就诊」置为「就诊中」。
     * <p>
     * 与叫号共用同一套约束：必须是当天当前时段的号，且该医生手头没有正在就诊的患者。
     * 否则可以绕过叫号直接接诊，一医多人的限制就形同虚设。
     */
    public BizRegistration receive(Long id) {
        BizRegistration registration = requireRegistration(id);
        int status = registration.getStatus() == null ? 0 : registration.getStatus();
        if (status != 0) {
            throw BizException.badRequest(status == 1 ? "该患者已在就诊中" : "该挂号状态不能接诊");
        }
        requireCallableSchedule(registration);
        assertDoctorIdle(registration);
        BizRegistration update = new BizRegistration();
        update.setId(id);
        update.setStatus(1);
        bizRegistrationMapper.updateById(update);
        registration.setStatus(1);
        return registration;
    }

    /**
     * 完成就诊：仅「就诊中」可完成。
     */
    public void complete(Long id) {
        BizRegistration registration = requireRegistration(id);
        int status = registration.getStatus() == null ? 0 : registration.getStatus();
        if (status == 0) {
            throw BizException.badRequest("该患者尚未接诊，不能完成就诊");
        }
        if (status != 1) {
            throw BizException.badRequest(status == 3 ? "该挂号已退号，不能完成就诊" : "该挂号已完成就诊");
        }
        BizRegistration update = new BizRegistration();
        update.setId(id);
        update.setStatus(2);
        bizRegistrationMapper.updateById(update);
    }

    /**
     * 生成排队号：优先用 Redis 自增，Redis 不可用时退回数据库计数，保证挂号不因缓存故障而失败。
     */
    private int nextQueueNo(Long deptId) {
        String key = QUEUE_KEY_PREFIX + "no:" + deptId + ":" + LocalDate.now();
        return nextSeq(key, () -> {
            LocalDateTime start = LocalDate.now().atStartOfDay();
            long count = bizRegistrationMapper.selectCount(Wrappers.<BizRegistration>lambdaQuery()
                    .eq(BizRegistration::getDeptId, deptId)
                    .ge(BizRegistration::getRegTime, start)
                    .lt(BizRegistration::getRegTime, start.plusDays(1)));
            return (int) count + 1;
        });
    }

    /**
     * 生成叫号序号，降级策略同上。
     */
    private int nextCallNo(Long deptId) {
        String key = QUEUE_KEY_PREFIX + "call:" + deptId + ":" + LocalDate.now();
        return nextSeq(key, () -> {
            LocalDateTime start = LocalDate.now().atStartOfDay();
            long count = bizCallRecordMapper.selectCount(Wrappers.<BizCallRecord>lambdaQuery()
                    .eq(BizCallRecord::getDeptId, deptId)
                    .ge(BizCallRecord::getCallTime, start)
                    .lt(BizCallRecord::getCallTime, start.plusDays(1)));
            return (int) count + 1;
        });
    }

    private int nextSeq(String key, IntSupplier fallback) {
        try {
            Long value = stringRedisTemplate.opsForValue().increment(key);
            if (value != null && value == 1L) {
                stringRedisTemplate.expire(key, Duration.ofDays(2));
            }
            if (value != null) {
                return value.intValue();
            }
        } catch (Exception e) {
            log.warn("Redis 不可用，队列号改用数据库计数兜底: {}", e.getMessage());
        }
        return fallback.getAsInt();
    }

    private BizSchedule requireSchedule(Long id) {
        BizSchedule schedule = bizScheduleMapper.selectById(id);
        if (schedule == null) {
            throw BizException.badRequest("排班不存在");
        }
        return schedule;
    }

    private BizRegistration requireRegistration(Long id) {
        BizRegistration registration = bizRegistrationMapper.selectById(id);
        if (registration == null) {
            throw BizException.badRequest("挂号记录不存在");
        }
        return registration;
    }
}
