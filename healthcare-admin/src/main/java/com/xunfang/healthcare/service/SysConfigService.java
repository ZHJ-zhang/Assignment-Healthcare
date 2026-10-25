package com.xunfang.healthcare.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.util.StrUtil;
import com.xunfang.healthcare.common.BizException;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.entity.SysConfig;
import com.xunfang.healthcare.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统参数管理。
 */
@Service
@RequiredArgsConstructor
public class SysConfigService {

    private final SysConfigMapper sysConfigMapper;

    /**
     * 参数分页。
     */
    public PageResult<SysConfig> page(PageQuery query) {
        Page<SysConfig> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(sysConfigMapper.selectPage(page, Wrappers.<SysConfig>lambdaQuery()
                .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                        .like(SysConfig::getConfigName, query.getKeyword()).or()
                        .like(SysConfig::getConfigKey, query.getKeyword()))
                .orderByDesc(SysConfig::getId)));
    }

    /**
     * 全部参数。
     */
    public List<SysConfig> list() {
        return sysConfigMapper.selectList(Wrappers.<SysConfig>lambdaQuery().orderByDesc(SysConfig::getId));
    }

    /**
     * 按键取值。
     */
    public String getValue(String configKey, String defaultValue) {
        SysConfig config = sysConfigMapper.selectOne(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getConfigKey, configKey).last("limit 1"));
        return config == null || config.getConfigValue() == null ? defaultValue : config.getConfigValue();
    }

    /**
     * 保存参数。
     */
    public SysConfig save(SysConfig config) {
        if (config.getId() == null) {
            if (sysConfigMapper.selectCount(Wrappers.<SysConfig>lambdaQuery()
                    .eq(SysConfig::getConfigKey, config.getConfigKey())) > 0) {
                throw BizException.badRequest("参数键已存在");
            }
            if (config.getBuiltIn() == null) {
                config.setBuiltIn(0);
            }
            sysConfigMapper.insert(config);
        } else {
            sysConfigMapper.updateById(config);
        }
        return config;
    }

    /**
     * 删除参数。
     */
    public void delete(Long id) {
        SysConfig config = sysConfigMapper.selectById(id);
        if (config == null) {
            throw BizException.badRequest("参数不存在");
        }
        if (config.getBuiltIn() != null && config.getBuiltIn() == 1) {
            throw BizException.badRequest("内置参数不允许删除");
        }
        sysConfigMapper.deleteById(id);
    }
}
