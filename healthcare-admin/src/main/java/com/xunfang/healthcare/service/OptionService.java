package com.xunfang.healthcare.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xunfang.healthcare.entity.BizDrug;
import com.xunfang.healthcare.entity.BizPatient;
import com.xunfang.healthcare.entity.BizRegistration;
import com.xunfang.healthcare.entity.SysDept;
import com.xunfang.healthcare.entity.SysRole;
import com.xunfang.healthcare.entity.SysUser;
import com.xunfang.healthcare.entity.SysUserRole;
import com.xunfang.healthcare.mapper.BizDrugMapper;
import com.xunfang.healthcare.mapper.BizPatientMapper;
import com.xunfang.healthcare.mapper.BizRegistrationMapper;
import com.xunfang.healthcare.mapper.SysDeptMapper;
import com.xunfang.healthcare.mapper.SysRoleMapper;
import com.xunfang.healthcare.mapper.SysUserMapper;
import com.xunfang.healthcare.mapper.SysUserRoleMapper;
import com.xunfang.healthcare.security.DataScopeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务下拉选项。
 * <p>医生、护士、收费员在开方、挂号、接诊时都需要「科室 / 医生 / 药品 / 患者」这几类选项，
 * 但它们对应的管理接口（/system/dept、/system/user、/drug/page）都要求管理类权限，
 * 直接调用会被 403 挡掉，页面上表现为「无权限」且下拉全空。
 * 这里单独开一组只读、登录即可访问的轻量接口，且只返回挑选字段，
 * 不把用户密码、患者身份证之类的敏感信息带到前端。</p>
 */
@Service
@RequiredArgsConstructor
public class OptionService {

    /** 选项接口一次最多返回的条数，避免把整张表倒给前端。 */
    private static final int MAX_ROWS = 500;

    private final SysDeptMapper sysDeptMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final BizDrugMapper bizDrugMapper;
    private final BizPatientMapper bizPatientMapper;
    private final BizRegistrationMapper bizRegistrationMapper;
    private final DataScopeHelper dataScopeHelper;

    /**
     * 科室选项。
     */
    public List<Map<String, Object>> listDepts() {
        List<SysDept> rows = sysDeptMapper.selectList(Wrappers.<SysDept>lambdaQuery()
                .eq(SysDept::getStatus, 1)
                .orderByAsc(SysDept::getSort));
        return rows.stream().map(dept -> {
            Map<String, Object> item = new LinkedHashMap<>(4);
            item.put("id", dept.getId());
            item.put("deptName", dept.getDeptName());
            item.put("parentId", dept.getParentId());
            return item;
        }).toList();
    }

    /**
     * 医生选项：只取具备 doctor 角色的启用账号。
     */
    public List<Map<String, Object>> listDoctors() {
        SysRole doctorRole = sysRoleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, "doctor")
                .last("limit 1"));
        if (doctorRole == null) {
            return Collections.emptyList();
        }
        List<Long> userIds = sysUserRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getRoleId, doctorRole.getId()))
                .stream()
                .map(SysUserRole::getUserId)
                .toList();
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysUser> users = sysUserMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .in(SysUser::getId, userIds)
                .eq(SysUser::getStatus, 1)
                .orderByAsc(SysUser::getId));
        return users.stream().map(user -> {
            Map<String, Object> item = new LinkedHashMap<>(4);
            item.put("id", user.getId());
            item.put("realName", user.getRealName() == null ? user.getUsername() : user.getRealName());
            item.put("deptId", user.getDeptId());
            return item;
        }).toList();
    }

    /**
     * 药品选项：开处方、收费时选药要用到名称、规格、单位与售价。
     */
    public List<Map<String, Object>> listDrugs(String keyword) {
        List<BizDrug> rows = bizDrugMapper.selectList(Wrappers.<BizDrug>lambdaQuery()
                .eq(BizDrug::getStatus, 1)
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                        .like(BizDrug::getDrugName, keyword).or()
                        .like(BizDrug::getDrugCode, keyword))
                .orderByAsc(BizDrug::getId)
                .last("limit " + MAX_ROWS));
        return rows.stream().map(drug -> {
            Map<String, Object> item = new LinkedHashMap<>(8);
            item.put("id", drug.getId());
            item.put("drugName", drug.getDrugName());
            item.put("spec", drug.getSpec());
            item.put("unit", drug.getUnit());
            item.put("salePrice", drug.getSalePrice());
            return item;
        }).toList();
    }

    /**
     * 患者选项：只返回门诊号、姓名与性别，不含身份证/手机号等敏感信息。
     * <p>
     * 医生的数据范围是"仅本人"，这里也只能挑出挂在他名下的患者：
     * 下拉框如果还是全院患者，等于把别的科室、别的医生的病患名单直接摊开。
     */
    public List<Map<String, Object>> listPatients(String keyword) {
        Long scopeUserId = dataScopeHelper.scopeUserId();
        List<Long> ownPatientIds = null;
        if (scopeUserId != null) {
            ownPatientIds = bizRegistrationMapper.selectList(Wrappers.<BizRegistration>lambdaQuery()
                            .select(BizRegistration::getPatientId)
                            .eq(BizRegistration::getDoctorId, scopeUserId)
                            .groupBy(BizRegistration::getPatientId))
                    .stream()
                    .map(BizRegistration::getPatientId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
            if (ownPatientIds.isEmpty()) {
                return Collections.emptyList();
            }
        }
        List<BizPatient> rows = bizPatientMapper.selectList(Wrappers.<BizPatient>lambdaQuery()
                .in(ownPatientIds != null, BizPatient::getId, ownPatientIds)
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                        .like(BizPatient::getName, keyword).or()
                        .like(BizPatient::getPatientNo, keyword))
                .orderByDesc(BizPatient::getCreateTime)
                .last("limit " + MAX_ROWS));
        return rows.stream().map(patient -> {
            Map<String, Object> item = new LinkedHashMap<>(4);
            item.put("id", patient.getId());
            item.put("name", patient.getName());
            item.put("patientNo", patient.getPatientNo());
            item.put("gender", patient.getGender());
            return item;
        }).toList();
    }
}
