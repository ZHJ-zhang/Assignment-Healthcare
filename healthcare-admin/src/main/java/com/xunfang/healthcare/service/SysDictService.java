package com.xunfang.healthcare.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.hutool.core.util.StrUtil;
import com.xunfang.healthcare.common.BizException;
import com.xunfang.healthcare.entity.SysDictItem;
import com.xunfang.healthcare.entity.SysDictType;
import com.xunfang.healthcare.mapper.SysDictItemMapper;
import com.xunfang.healthcare.mapper.SysDictTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据字典管理。
 */
@Service
@RequiredArgsConstructor
public class SysDictService {

    private final SysDictTypeMapper sysDictTypeMapper;
    private final SysDictItemMapper sysDictItemMapper;

    /**
     * 字典类型列表。
     */
    public List<SysDictType> listTypes() {
        return sysDictTypeMapper.selectList(Wrappers.<SysDictType>lambdaQuery()
                .orderByAsc(SysDictType::getId));
    }

    /**
     * 按类型查询字典项。
     */
    public List<SysDictItem> listItems(String dictType) {
        return sysDictItemMapper.selectList(Wrappers.<SysDictItem>lambdaQuery()
                .eq(SysDictItem::getDictType, dictType)
                .eq(SysDictItem::getStatus, 1)
                .orderByAsc(SysDictItem::getSort));
    }

    /**
     * 全部字典项（字典管理页面使用）。
     */
    public List<SysDictItem> listAllItems(String dictType) {
        return sysDictItemMapper.selectList(Wrappers.<SysDictItem>lambdaQuery()
                .eq(StrUtil.isNotBlank(dictType), SysDictItem::getDictType, dictType)
                .orderByAsc(SysDictItem::getDictType)
                .orderByAsc(SysDictItem::getSort));
    }

    /**
     * 保存字典类型（新增或修改）。
     */
    public SysDictType saveType(SysDictType type) {
        if (type.getId() == null) {
            if (sysDictTypeMapper.selectCount(Wrappers.<SysDictType>lambdaQuery()
                    .eq(SysDictType::getDictType, type.getDictType())) > 0) {
                throw BizException.badRequest("字典类型已存在");
            }
            if (type.getStatus() == null) {
                type.setStatus(1);
            }
            sysDictTypeMapper.insert(type);
        } else {
            sysDictTypeMapper.updateById(type);
        }
        return type;
    }

    /**
     * 保存字典项。
     */
    public SysDictItem saveItem(SysDictItem item) {
        if (item.getId() == null) {
            if (item.getStatus() == null) {
                item.setStatus(1);
            }
            sysDictItemMapper.insert(item);
        } else {
            sysDictItemMapper.updateById(item);
        }
        return item;
    }

    /**
     * 删除字典类型及其字典项。
     */
    public void deleteType(Long id) {
        SysDictType type = sysDictTypeMapper.selectById(id);
        if (type == null) {
            throw BizException.badRequest("字典类型不存在");
        }
        sysDictTypeMapper.deleteById(id);
        sysDictItemMapper.delete(Wrappers.<SysDictItem>lambdaQuery().eq(SysDictItem::getDictType, type.getDictType()));
    }

    /**
     * 删除字典项。
     */
    public void deleteItem(Long id) {
        if (sysDictItemMapper.selectById(id) == null) {
            throw BizException.badRequest("字典项不存在");
        }
        sysDictItemMapper.deleteById(id);
    }
}
