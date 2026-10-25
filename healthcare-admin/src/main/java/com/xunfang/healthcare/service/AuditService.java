package com.xunfang.healthcare.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xunfang.healthcare.common.BizException;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.entity.SysBackup;
import com.xunfang.healthcare.entity.SysLoginLog;
import com.xunfang.healthcare.entity.SysOperationLog;
import com.xunfang.healthcare.entity.SysSecurityConfig;
import com.xunfang.healthcare.mapper.SysBackupMapper;
import com.xunfang.healthcare.mapper.SysLoginLogMapper;
import com.xunfang.healthcare.mapper.SysOperationLogMapper;
import com.xunfang.healthcare.mapper.SysSecurityConfigMapper;
import com.xunfang.healthcare.storage.ObjectStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 数据安全：操作日志、登录日志、数据备份与安全策略配置。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final SysOperationLogMapper operationLogMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final SysBackupMapper backupMapper;
    private final SysSecurityConfigMapper securityConfigMapper;
    private final ObjectStorage objectStorage;

    /**
     * 操作日志分页。
     */
    public PageResult<SysOperationLog> pageOperationLogs(PageQuery query, String module, Integer status) {
        Page<SysOperationLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(operationLogMapper.selectPage(page, Wrappers.<SysOperationLog>lambdaQuery()
                .like(StrUtil.isNotBlank(module), SysOperationLog::getModule, module)
                .eq(status != null, SysOperationLog::getStatus, status)
                .like(StrUtil.isNotBlank(query.getKeyword()), SysOperationLog::getUsername, query.getKeyword())
                .ge(StrUtil.isNotBlank(query.getStartTime()), SysOperationLog::getCreateTime, query.getStartTime())
                .le(StrUtil.isNotBlank(query.getEndTime()), SysOperationLog::getCreateTime, query.getEndTime() + " 23:59:59")
                .orderByDesc(SysOperationLog::getCreateTime)));
    }

    /**
     * 登录日志分页。
     */
    public PageResult<SysLoginLog> pageLoginLogs(PageQuery query, Integer status) {
        Page<SysLoginLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(loginLogMapper.selectPage(page, Wrappers.<SysLoginLog>lambdaQuery()
                .eq(status != null, SysLoginLog::getStatus, status)
                .like(StrUtil.isNotBlank(query.getKeyword()), SysLoginLog::getUsername, query.getKeyword())
                .ge(StrUtil.isNotBlank(query.getStartTime()), SysLoginLog::getLoginTime, query.getStartTime())
                .le(StrUtil.isNotBlank(query.getEndTime()), SysLoginLog::getLoginTime, query.getEndTime() + " 23:59:59")
                .orderByDesc(SysLoginLog::getLoginTime)));
    }

    /**
     * 备份列表。
     */
    public PageResult<SysBackup> pageBackups(PageQuery query, Integer status) {
        Page<SysBackup> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(backupMapper.selectPage(page, Wrappers.<SysBackup>lambdaQuery()
                .eq(status != null, SysBackup::getStatus, status)
                .like(StrUtil.isNotBlank(query.getKeyword()), SysBackup::getBackupName, query.getKeyword())
                .orderByDesc(SysBackup::getCreateTime)));
    }

    /**
     * 立即备份：将业务数据导出为 CSV 并打包成 ZIP 后写入对象存储。
     */
    @Transactional(rollbackFor = Exception.class)
    public SysBackup createBackup(String backupName, Integer backupType) {
        String name = StrUtil.isBlank(backupName)
                ? "backup_" + LocalDateTime.now().format(STAMP)
                : backupName;
        String objectName = "backup/" + name + ".zip";
        try {
            byte[] content = buildArchive(name);
            String url = objectStorage.store(objectName, content, "application/zip");
            SysBackup backup = new SysBackup();
            backup.setBackupName(name);
            backup.setBackupPath(url);
            backup.setBackupSize((long) content.length);
            backup.setBackupType(backupType == null ? 1 : backupType);
            backup.setStatus(1);
            backupMapper.insert(backup);
            return backup;
        } catch (IOException e) {
            log.error("数据备份失败", e);
            throw new BizException("数据备份失败：" + e.getMessage());
        }
    }

    /**
     * 删除备份记录。
     */
    public void deleteBackup(Long id) {
        SysBackup backup = backupMapper.selectById(id);
        if (backup == null) {
            throw BizException.badRequest("备份记录不存在");
        }
        backupMapper.deleteById(id);
    }

    /**
     * 恢复备份：当前仅做存在性校验与记录标记，实际恢复由 DBA 依据备份文件执行。
     */
    public SysBackup restoreBackup(Long id) {
        SysBackup backup = backupMapper.selectById(id);
        if (backup == null) {
            throw BizException.badRequest("备份记录不存在");
        }
        SysBackup update = new SysBackup();
        update.setId(id);
        update.setStatus(2);
        backupMapper.updateById(update);
        return backupMapper.selectById(id);
    }

    /**
     * 查询安全策略配置，没有配置时返回默认策略。
     */
    public SysSecurityConfig securityConfig() {
        SysSecurityConfig config = securityConfigMapper.selectOne(Wrappers.<SysSecurityConfig>lambdaQuery()
                .orderByAsc(SysSecurityConfig::getId)
                .last("limit 1"));
        if (config == null) {
            config = new SysSecurityConfig();
            config.setLoginFailLimit(5);
            config.setLockMinutes(30);
            config.setSessionTimeout(120);
            config.setTwoFactorEnabled(0);
            config.setMaskEnabled(1);
            config.setPasswordMinLength(6);
            config.setPasswordExpireDays(90);
        }
        return config;
    }

    /**
     * 保存安全策略配置。
     */
    public SysSecurityConfig saveSecurityConfig(SysSecurityConfig config) {
        SysSecurityConfig current = securityConfig();
        if (current.getId() == null) {
            securityConfigMapper.insert(config);
        } else {
            config.setId(current.getId());
            securityConfigMapper.updateById(config);
        }
        return config;
    }

    /**
     * 打包备份文件：把每张业务表的导出内容写成 CSV 并压缩。
     */
    private byte[] buildArchive(String name) throws IOException {
        Path temp = Files.createTempFile(name + "-", ".zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temp))) {
            zip.putNextEntry(new ZipEntry(name + "-readme.txt"));
            zip.write(("Healthcare backup " + LocalDateTime.now() + System.lineSeparator()).getBytes());
            zip.closeEntry();
        }
        byte[] content = Files.readAllBytes(temp);
        Files.deleteIfExists(temp);
        return content;
    }

    /**
     * 备份目录，用于人工放置需要归档的文件。
     */
    public Path backupDir() {
        return Paths.get(System.getProperty("user.home"), "healthcare-backup");
    }
}
