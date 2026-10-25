package com.xunfang.healthcare.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xunfang.healthcare.common.BizException;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.entity.SysMessage;
import com.xunfang.healthcare.entity.SysNotice;
import com.xunfang.healthcare.entity.SysSmsRecord;
import com.xunfang.healthcare.entity.BizCriticalValue;
import com.xunfang.healthcare.entity.BizStock;
import com.xunfang.healthcare.mapper.BizCriticalValueMapper;
import com.xunfang.healthcare.mapper.BizStockMapper;
import com.xunfang.healthcare.mapper.SysMessageMapper;
import com.xunfang.healthcare.mapper.SysNoticeMapper;
import com.xunfang.healthcare.mapper.SysSmsRecordMapper;
import com.xunfang.healthcare.mq.LocalSmsMessageHandler;
import com.xunfang.healthcare.mq.MessagePublisher;
import com.xunfang.healthcare.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 站内消息、公告通知与短信发送记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final SysMessageMapper messageMapper;
    private final SysNoticeMapper noticeMapper;
    private final SysSmsRecordMapper smsRecordMapper;
    private final BizCriticalValueMapper criticalValueMapper;
    private final BizStockMapper stockMapper;
    private final MessagePublisher messagePublisher;

    /**
     * 我的消息分页。
     */
    public PageResult<SysMessage> pageMessages(PageQuery query, Integer isRead, Long receiverId) {
        Long target = receiverId == null ? SecurityUtils.getUserIdOrNull() : receiverId;
        Page<SysMessage> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(messageMapper.selectPage(page, Wrappers.<SysMessage>lambdaQuery()
                .eq(target != null, SysMessage::getReceiverId, target)
                .eq(isRead != null, SysMessage::getIsRead, isRead)
                .like(StrUtil.isNotBlank(query.getKeyword()), SysMessage::getTitle, query.getKeyword())
                .orderByDesc(SysMessage::getCreateTime)));
    }

    /**
     * 未读消息数量。
     */
    public long unreadCount() {
        Long userId = SecurityUtils.getUserIdOrNull();
        if (userId == null) {
            return 0L;
        }
        return messageMapper.selectCount(Wrappers.<SysMessage>lambdaQuery()
                .eq(SysMessage::getReceiverId, userId)
                .eq(SysMessage::getIsRead, 0));
    }

    /**
     * 标记已读。
     */
    public void readMessage(Long id) {
        SysMessage update = new SysMessage();
        update.setId(id);
        update.setIsRead(1);
        messageMapper.updateById(update);
    }

    /**
     * 全部标记已读。
     */
    public int readAll() {
        Long userId = SecurityUtils.getUserIdOrNull();
        if (userId == null) {
            return 0;
        }
        SysMessage update = new SysMessage();
        update.setIsRead(1);
        return messageMapper.update(update, Wrappers.<SysMessage>lambdaUpdate()
                .eq(SysMessage::getReceiverId, userId)
                .eq(SysMessage::getIsRead, 0));
    }

    /**
     * 发送站内消息。
     */
    public void sendMessage(Long receiverId, String title, String content, Integer msgType, String bizId) {
        Map<String, Object> payload = new HashMap<>(6);
        payload.put("receiverId", receiverId);
        payload.put("title", title);
        payload.put("content", content);
        payload.put("msgType", msgType == null ? 1 : msgType);
        payload.put("bizId", bizId);
        messagePublisher.publishAfterCommit("message", payload);
    }

    /**
     * 公告分页。
     */
    public PageResult<SysNotice> pageNotices(PageQuery query, Integer noticeType, Integer status) {
        Page<SysNotice> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(noticeMapper.selectPage(page, Wrappers.<SysNotice>lambdaQuery()
                .eq(noticeType != null, SysNotice::getNoticeType, noticeType)
                .eq(status != null, SysNotice::getStatus, status)
                .like(StrUtil.isNotBlank(query.getKeyword()), SysNotice::getTitle, query.getKeyword())
                .orderByDesc(SysNotice::getPublishTime)));
    }

    /**
     * 发布公告。
     */
    @Transactional(rollbackFor = Exception.class)
    public SysNotice publishNotice(SysNotice notice) {
        if (notice.getId() == null) {
            notice.setPublisherId(SecurityUtils.getUserIdOrNull());
            notice.setPublishTime(LocalDateTime.now());
            if (notice.getStatus() == null) {
                notice.setStatus(1);
            }
            noticeMapper.insert(notice);
        } else {
            noticeMapper.updateById(notice);
        }
        return notice;
    }

    /**
     * 删除公告。
     */
    public void deleteNotice(Long id) {
        if (noticeMapper.selectById(id) == null) {
            throw BizException.badRequest("公告不存在");
        }
        noticeMapper.deleteById(id);
    }

    /**
     * 短信记录分页。
     */
    public PageResult<SysSmsRecord> pageSms(PageQuery query, Integer sendStatus) {
        Page<SysSmsRecord> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(smsRecordMapper.selectPage(page, Wrappers.<SysSmsRecord>lambdaQuery()
                .eq(sendStatus != null, SysSmsRecord::getSendStatus, sendStatus)
                .like(StrUtil.isNotBlank(query.getKeyword()), SysSmsRecord::getPhone, query.getKeyword())
                .orderByDesc(SysSmsRecord::getSendTime)));
    }

    /**
     * 发送短信：写入发送记录并投递到消息队列。
     */
    @Transactional(rollbackFor = Exception.class)
    public SysSmsRecord sendSms(String phone, String templateCode, String content) {
        if (StrUtil.isBlank(phone)) {
            throw BizException.badRequest("手机号不能为空");
        }
        SysSmsRecord record = new SysSmsRecord();
        record.setPhone(phone);
        record.setTemplateCode(templateCode);
        record.setContent(content);
        record.setSendStatus(LocalSmsMessageHandler.STATUS_SENDING);
        record.setSendTime(LocalDateTime.now());
        smsRecordMapper.insert(record);

        Map<String, Object> payload = new HashMap<>(4);
        payload.put("phone", phone);
        payload.put("templateCode", templateCode);
        payload.put("content", content);
        payload.put("recordId", record.getId());
        messagePublisher.publishAfterCommit("sms", payload);
        return record;
    }

    /**
     * 预警列表：需要处理的危急值与库存预警合并视图。
     */
    public List<Map<String, Object>> warningList(int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (BizCriticalValue value : criticalValueMapper.selectList(
                Wrappers.<BizCriticalValue>lambdaQuery()
                        .eq(BizCriticalValue::getStatus, 0)
                        .orderByDesc(BizCriticalValue::getCreateTime)
                        .last("limit " + limit))) {
            Map<String, Object> item = new HashMap<>(6);
            item.put("type", "critical");
            item.put("typeName", "危急值");
            item.put("title", value.getItemName() + " " + value.getValue());
            item.put("content", "参考范围：" + value.getReferenceRange());
            item.put("bizId", value.getId());
            item.put("createTime", value.getCreateTime());
            list.add(item);
        }
        for (BizStock stock : stockMapper.selectList(Wrappers.<BizStock>lambdaQuery()
                .apply("quantity <= warn_num")
                .orderByAsc(BizStock::getQuantity)
                .last("limit " + limit))) {
            Map<String, Object> item = new HashMap<>(6);
            item.put("type", "stock");
            item.put("typeName", "库存预警");
            item.put("title", "药品 " + stock.getDrugId() + " 库存 " + stock.getQuantity());
            item.put("content", "批次号：" + stock.getBatchNo());
            item.put("bizId", stock.getId());
            item.put("createTime", stock.getUpdateTime());
            list.add(item);
        }
        return list;
    }
}
