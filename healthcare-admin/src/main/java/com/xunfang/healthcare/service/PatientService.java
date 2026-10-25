package com.xunfang.healthcare.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.util.StrUtil;
import com.xunfang.healthcare.common.BizException;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.entity.BizFollowUp;
import com.xunfang.healthcare.entity.BizMedicalRecord;
import com.xunfang.healthcare.entity.BizPatient;
import com.xunfang.healthcare.entity.BizPatientCard;
import com.xunfang.healthcare.mapper.BizFollowUpMapper;
import com.xunfang.healthcare.mapper.BizMedicalRecordMapper;
import com.xunfang.healthcare.mapper.BizPatientCardMapper;
import com.xunfang.healthcare.mapper.BizPatientMapper;
import com.xunfang.healthcare.security.SecurityUtils;
import com.xunfang.healthcare.util.BizNoGenerator;
import com.xunfang.healthcare.util.CryptoUtil;
import com.xunfang.healthcare.vo.PatientVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 患者档案、就诊卡、病历与随访管理。
 */
@Service
@RequiredArgsConstructor
public class PatientService {

    private final BizPatientMapper bizPatientMapper;
    private final BizPatientCardMapper bizPatientCardMapper;
    private final BizMedicalRecordMapper bizMedicalRecordMapper;
    private final BizFollowUpMapper bizFollowUpMapper;
    private final BizNoGenerator bizNoGenerator;
    private final NotifyService notifyService;

    /**
     * 患者分页查询。
     */
    public PageResult<PatientVO> page(PageQuery query, String gender, Integer status) {
        Page<BizPatient> page = new Page<>(query.getPageNum(), query.getPageSize());
        String keyword = query.getKeyword();
        Page<BizPatient> result = bizPatientMapper.selectPage(page, Wrappers.<BizPatient>lambdaQuery()
                .and(StrUtil.isNotBlank(keyword), wrapper -> wrapper
                        .like(BizPatient::getPatientNo, keyword).or()
                        .like(BizPatient::getName, keyword).or()
                        .like(BizPatient::getCardNo, keyword).or()
                        .eq(BizPatient::getPhoneHash, CryptoUtil.digest(keyword)).or()
                        .eq(BizPatient::getIdCardHash, CryptoUtil.digest(keyword)))
                .eq(gender != null, BizPatient::getGender, gender)
                .eq(status != null, BizPatient::getStatus, status)
                .ge(StrUtil.isNotBlank(query.getStartTime()),
                        BizPatient::getCreateTime, query.getStartTime())
                .le(StrUtil.isNotBlank(query.getEndTime()),
                        BizPatient::getCreateTime, query.getEndTime() + " 23:59:59")
                .orderByDesc(BizPatient::getCreateTime));

        PageResult<PatientVO> pageResult = new PageResult<>(result.getTotal(), result.getCurrent(),
                result.getSize(), result.getRecords().stream().map(this::toVO).toList());
        return pageResult;
    }

    /**
     * 患者详情。
     */
    public PatientVO detail(Long id) {
        return toVO(requirePatient(id));
    }

    /**
     * 新增患者。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizPatient create(BizPatient patient) {
        patient.setId(null);
        if (StrUtil.isBlank(patient.getPatientNo())) {
            patient.setPatientNo(bizNoGenerator.next("P"));
        }
        if (patient.getStatus() == null) {
            patient.setStatus(1);
        }
        if (patient.getCardBalance() == null) {
            patient.setCardBalance(java.math.BigDecimal.ZERO);
        }
        if (patient.getDeptId() == null) {
            patient.setDeptId(SecurityUtils.getDeptId());
        }
        encryptSensitive(patient);
        bizPatientMapper.insert(patient);
        return patient;
    }

    /**
     * 修改患者。
     */
    public BizPatient update(BizPatient patient) {
        requirePatient(patient.getId());
        encryptSensitive(patient);
        bizPatientMapper.updateById(patient);
        return patient;
    }

    /**
     * 删除患者。
     */
    public void delete(Long id) {
        requirePatient(id);
        bizPatientMapper.deleteById(id);
    }

    /**
     * 查询患者病历列表。
     */
    public List<BizMedicalRecord> listRecords(Long patientId) {
        return bizMedicalRecordMapper.selectList(Wrappers.<BizMedicalRecord>lambdaQuery()
                .eq(BizMedicalRecord::getPatientId, patientId)
                .orderByDesc(BizMedicalRecord::getVisitTime));
    }

    /**
     * 保存病历。
     */
    public BizMedicalRecord saveRecord(BizMedicalRecord record) {
        if (record.getId() == null) {
            if (record.getPatientId() == null) {
                throw BizException.badRequest("患者不能为空");
            }
            if (record.getVisitTime() == null) {
                record.setVisitTime(java.time.LocalDateTime.now());
            }
            if (record.getStatus() == null) {
                record.setStatus(0);
            }
            if (record.getDoctorId() == null) {
                record.setDoctorId(SecurityUtils.getUserIdOrNull());
            }
            bizMedicalRecordMapper.insert(record);
        } else {
            bizMedicalRecordMapper.updateById(record);
        }
        return record;
    }

    /**
     * 就诊卡列表。
     */
    public List<BizPatientCard> listCards(Long patientId) {
        return bizPatientCardMapper.selectList(Wrappers.<BizPatientCard>lambdaQuery()
                .eq(patientId != null, BizPatientCard::getPatientId, patientId)
                .orderByDesc(BizPatientCard::getIssueTime));
    }

    /**
     * 发卡。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizPatientCard issueCard(BizPatientCard card) {
        if (card.getPatientId() == null) {
            throw BizException.badRequest("患者不能为空");
        }
        BizPatient patient = requirePatient(card.getPatientId());
        card.setId(null);
        if (StrUtil.isBlank(card.getCardNo())) {
            card.setCardNo(bizNoGenerator.next("C"));
        }
        if (card.getBalance() == null) {
            card.setBalance(java.math.BigDecimal.ZERO);
        }
        if (card.getStatus() == null) {
            card.setStatus(1);
        }
        card.setIssueTime(java.time.LocalDateTime.now());
        bizPatientCardMapper.insert(card);

        BizPatient update = new BizPatient();
        update.setId(patient.getId());
        update.setCardNo(card.getCardNo());
        update.setCardBalance(card.getBalance());
        bizPatientMapper.updateById(update);
        return card;
    }

    /**
     * 挂失就诊卡：只有正常状态的卡可以挂失。
     */
    public void lossCard(Long id) {
        BizPatientCard card = bizPatientCardMapper.selectById(id);
        if (card == null) {
            throw BizException.badRequest("就诊卡不存在");
        }
        int status = card.getStatus() == null ? 1 : card.getStatus();
        if (status == 2) {
            throw BizException.badRequest("该卡已注销，无需挂失");
        }
        if (status == 0) {
            throw BizException.badRequest("该卡已挂失，无需重复操作");
        }
        BizPatientCard update = new BizPatientCard();
        update.setId(id);
        update.setStatus(0);
        update.setLossTime(java.time.LocalDateTime.now());
        bizPatientCardMapper.updateById(update);
    }

    /**
     * 补办就诊卡：挂失原卡并发放新卡。
     * <p>
     * 只有「患者当前持有的那张卡」才能补办。否则对同一张已挂失的旧卡反复点补办，
     * 会不断复制余额生成多张等额新卡。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizPatientCard reissueCard(Long id, String remark) {
        BizPatientCard card = bizPatientCardMapper.selectById(id);
        if (card == null) {
            throw BizException.badRequest("就诊卡不存在");
        }
        if (card.getStatus() != null && card.getStatus() == 2) {
            throw BizException.badRequest("该卡已注销，不能补办");
        }
        BizPatient patient = bizPatientMapper.selectById(card.getPatientId());
        if (patient == null) {
            throw BizException.badRequest("患者不存在");
        }
        if (patient.getCardNo() != null && !patient.getCardNo().equals(card.getCardNo())) {
            throw BizException.badRequest("该卡已被补办，请选择患者当前有效的就诊卡操作");
        }
        BizPatientCard update = new BizPatientCard();
        update.setId(id);
        update.setStatus(0);
        update.setLossTime(java.time.LocalDateTime.now());
        update.setRemark(remark);
        bizPatientCardMapper.updateById(update);

        BizPatientCard newCard = new BizPatientCard();
        newCard.setPatientId(card.getPatientId());
        newCard.setCardNo(bizNoGenerator.next("C"));
        newCard.setCardType(card.getCardType());
        newCard.setBalance(card.getBalance());
        newCard.setStatus(1);
        newCard.setIssueTime(java.time.LocalDateTime.now());
        newCard.setRemark("补办自 " + card.getCardNo());
        bizPatientCardMapper.insert(newCard);

        BizPatient patientUpdate = new BizPatient();
        patientUpdate.setId(card.getPatientId());
        patientUpdate.setCardNo(newCard.getCardNo());
        bizPatientMapper.updateById(patientUpdate);
        return newCard;
    }

    /**
     * 注销就诊卡：卡内余额清零并退回，患者档案上的卡号一并清空。
     * <p>
     * 注销是终态，不能撤销；注销后该卡号不再可用，再就诊需要重新办卡。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelCard(Long id, String reason) {
        BizPatientCard card = bizPatientCardMapper.selectById(id);
        if (card == null) {
            throw BizException.badRequest("就诊卡不存在");
        }
        int status = card.getStatus() == null ? 1 : card.getStatus();
        if (status == 2) {
            throw BizException.badRequest("该卡已注销，无需重复操作");
        }
        BigDecimal balance = card.getBalance() == null ? BigDecimal.ZERO : card.getBalance();
        StringBuilder remark = new StringBuilder(StrUtil.isBlank(reason) ? "注销就诊卡" : reason);
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            remark.append("；注销退余额 ￥").append(balance.setScale(2, RoundingMode.HALF_UP));
        }

        BizPatientCard update = new BizPatientCard();
        update.setId(id);
        update.setStatus(2);
        update.setBalance(BigDecimal.ZERO);
        update.setRemark(remark.toString());
        bizPatientCardMapper.updateById(update);

        BizPatient patient = bizPatientMapper.selectById(card.getPatientId());
        if (patient != null && StrUtil.isNotBlank(patient.getCardNo())
                && patient.getCardNo().equals(card.getCardNo())) {
            bizPatientMapper.update(null, Wrappers.<BizPatient>lambdaUpdate()
                    .eq(BizPatient::getId, patient.getId())
                    .set(BizPatient::getCardNo, null)
                    .set(BizPatient::getCardBalance, BigDecimal.ZERO));
        }
    }

    /**
     * 随访分页。
     */
    public PageResult<BizFollowUp> pageFollowUp(PageQuery query, Long patientId, Integer status) {
        Page<BizFollowUp> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(bizFollowUpMapper.selectPage(page, Wrappers.<BizFollowUp>lambdaQuery()
                .eq(patientId != null, BizFollowUp::getPatientId, patientId)
                .eq(status != null, BizFollowUp::getStatus, status)
                .orderByDesc(BizFollowUp::getPlanDate)));
    }

    /**
     * 保存随访记录。
     */
    public BizFollowUp saveFollowUp(BizFollowUp followUp) {
        if (followUp.getId() == null) {
            if (followUp.getPatientId() == null) {
                throw BizException.badRequest("患者不能为空");
            }
            if (followUp.getStatus() == null) {
                followUp.setStatus(0);
            }
            if (followUp.getOperatorId() == null) {
                followUp.setOperatorId(SecurityUtils.getUserIdOrNull());
            }
            if (followUp.getDeptId() == null) {
                followUp.setDeptId(SecurityUtils.getDeptId());
            }
            bizFollowUpMapper.insert(followUp);
        } else {
            bizFollowUpMapper.updateById(followUp);
        }
        notifyFollowUpSaved(followUp);
        return followUp;
    }

    /**
     * 随访保存：短信提醒患者，给随访人发待办提醒。
     */
    private void notifyFollowUpSaved(BizFollowUp followUp) {
        if (followUp.getStatus() != null && followUp.getStatus() == 1) {
            return;
        }
        BizPatient patient = bizPatientMapper.selectById(followUp.getPatientId());
        if (patient == null) {
            return;
        }
        String planDate = followUp.getPlanDate() == null ? "待定" : followUp.getPlanDate().toString();
        notifyService.smsToPatient(patient, "FOLLOW_UP_REMIND",
                patient.getName() + "您好，您的随访计划已安排在" + planDate + "，届时将有医护人员与您联系。");

        Long operatorId = followUp.getOperatorId() == null ? SecurityUtils.getUserIdOrNull() : followUp.getOperatorId();
        notifyService.inboxToUser(operatorId, "随访待办提醒",
                "患者" + patient.getName() + "的随访计划日期为" + planDate + "，请及时完成随访。",
                2, String.valueOf(followUp.getId()));
    }

    /**
     * 待随访提醒（计划日期已到且未完成）。
     */
    public List<BizFollowUp> pendingFollowUps() {
        return bizFollowUpMapper.selectList(Wrappers.<BizFollowUp>lambdaQuery()
                .eq(BizFollowUp::getStatus, 0)
                .le(BizFollowUp::getPlanDate, LocalDate.now())
                .orderByAsc(BizFollowUp::getPlanDate));
    }

    private BizPatient requirePatient(Long id) {
        BizPatient patient = bizPatientMapper.selectById(id);
        if (patient == null) {
            throw BizException.badRequest("患者不存在");
        }
        return patient;
    }

    private PatientVO toVO(BizPatient patient) {
        PatientVO vo = new PatientVO();
        org.springframework.beans.BeanUtils.copyProperties(patient, vo);
        vo.setIdCard(CryptoUtil.decrypt(patient.getIdCard()));
        vo.setPhone(CryptoUtil.decrypt(patient.getPhone()));
        vo.setIdCardMasked(CryptoUtil.maskIdCard(vo.getIdCard()));
        vo.setPhoneMasked(CryptoUtil.maskPhone(vo.getPhone()));
        vo.setAge(calculateAge(patient.getBirthDate()));
        return vo;
    }

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private void encryptSensitive(BizPatient patient) {
        if (StrUtil.isNotBlank(patient.getIdCard())) {
            patient.setIdCardHash(CryptoUtil.digest(patient.getIdCard()));
            patient.setIdCard(CryptoUtil.encrypt(patient.getIdCard()));
        }
        if (StrUtil.isNotBlank(patient.getPhone())) {
            patient.setPhoneHash(CryptoUtil.digest(patient.getPhone()));
            patient.setPhone(CryptoUtil.encrypt(patient.getPhone()));
        }
    }
}
