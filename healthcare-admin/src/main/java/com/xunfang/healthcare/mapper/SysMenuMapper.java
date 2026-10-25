package com.xunfang.healthcare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xunfang.healthcare.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 查询用户拥有的菜单与按钮权限。
     */
    @Select("""
            SELECT DISTINCT m.* FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.id
            JOIN sys_user_role ur ON ur.role_id = rm.role_id
            JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.status = 1
            WHERE ur.user_id = #{userId} AND m.deleted = 0
            ORDER BY m.parent_id, m.sort
            """)
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);
}
