package com.xunfang.healthcare.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xunfang.healthcare.common.BizException;
import com.xunfang.healthcare.entity.SysDept;
import com.xunfang.healthcare.entity.SysUser;
import com.xunfang.healthcare.mapper.SysDeptMapper;
import com.xunfang.healthcare.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 科室（部门）管理。
 */
@Service
@RequiredArgsConstructor
public class SysDeptService {

    private final SysDeptMapper sysDeptMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 科室列表（平铺）。
     */
    public List<SysDept> list() {
        return sysDeptMapper.selectList(Wrappers.<SysDept>lambdaQuery()
                .orderByAsc(SysDept::getSort));
    }

    /**
     * 科室树。
     */
    public List<SysDept> tree() {
        List<SysDept> all = list();
        return buildTree(all, 0L);
    }

    /**
     * 科室详情。
     */
    public SysDept detail(Long id) {
        SysDept dept = sysDeptMapper.selectById(id);
        if (dept == null) {
            throw BizException.badRequest("科室不存在");
        }
        return dept;
    }

    /**
     * 新增科室。
     */
    public SysDept create(SysDept dept) {
        dept.setId(null);
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        if (dept.getStatus() == null) {
            dept.setStatus(1);
        }
        sysDeptMapper.insert(dept);
        return dept;
    }

    /**
     * 修改科室。
     */
    public SysDept update(SysDept dept) {
        detail(dept.getId());
        if (dept.getId().equals(dept.getParentId())) {
            throw BizException.badRequest("上级科室不能是自己");
        }
        sysDeptMapper.updateById(dept);
        return dept;
    }

    /**
     * 删除科室。
     */
    public void delete(Long id) {
        detail(id);
        if (sysDeptMapper.selectCount(Wrappers.<SysDept>lambdaQuery().eq(SysDept::getParentId, id)) > 0) {
            throw BizException.badRequest("存在下级科室，无法删除");
        }
        if (sysUserMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getDeptId, id)) > 0) {
            throw BizException.badRequest("科室下仍有用户，无法删除");
        }
        sysDeptMapper.deleteById(id);
    }

    private List<SysDept> buildTree(List<SysDept> all, Long parentId) {
        Map<Long, List<SysDept>> grouped = all.stream()
                .collect(Collectors.groupingBy(dept -> dept.getParentId() == null ? 0L : dept.getParentId()));
        List<SysDept> children = grouped.getOrDefault(parentId, new ArrayList<>());
        children.sort(Comparator.comparing((SysDept dept) -> dept.getSort() == null ? 0 : dept.getSort()));
        for (SysDept child : children) {
            child.setChildren(buildTree(all, child.getId()));
        }
        return children;
    }
}
