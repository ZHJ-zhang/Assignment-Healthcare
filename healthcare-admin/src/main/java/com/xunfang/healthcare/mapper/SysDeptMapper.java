package com.xunfang.healthcare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xunfang.healthcare.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 查询本部门及所有下级部门 ID，用于数据权限过滤。
     */
    @Select("""
            WITH RECURSIVE dept_tree AS (
                SELECT id FROM sys_dept WHERE id = #{deptId} AND deleted = 0
                UNION ALL
                SELECT d.id FROM sys_dept d
                JOIN dept_tree t ON d.parent_id = t.id
                WHERE d.deleted = 0
            )
            SELECT id FROM dept_tree
            """)
    List<Long> selectSelfAndDescendantIds(@Param("deptId") Long deptId);
}
