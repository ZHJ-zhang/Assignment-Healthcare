package com.xunfang.healthcare.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 业务单号生成器。
 *
 * <p>规则为「前缀 + 日期(yyyyMMdd) + 4 位流水号」，例如 REG202501010001。
 * 流水号由 Redis 自增保证并发下不重复，但 Redis 只是加速器而非事实来源：
 * Redis 重启、过期或被清空都会让计数器回到 1，因此每次生成前都会查一次
 * 数据库里当天已用的最大号，取两者的较大值，避免重复发号撞唯一索引。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BizNoGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private static final String KEY_PREFIX = "healthcare:seq:";

    private static final Duration KEY_TTL = Duration.ofDays(7);

    /**
     * 前缀 → {表名, 单号列}，用于回查数据库当日最大流水号。
     * 表名与列名是固定常量，不来自外部输入。
     */
    private static final Map<String, String[]> NO_COLUMNS = Map.ofEntries(
            Map.entry("P", new String[]{"biz_patient", "patient_no"}),
            Map.entry("C", new String[]{"biz_patient_card", "card_no"}),
            Map.entry("HR", new String[]{"biz_health_record", "record_no"}),
            Map.entry("REG", new String[]{"biz_registration", "reg_no"}),
            Map.entry("PR", new String[]{"biz_prescription", "presc_no"}),
            Map.entry("CH", new String[]{"biz_charge", "charge_no"}),
            Map.entry("RF", new String[]{"biz_refund", "refund_no"}),
            Map.entry("INV", new String[]{"biz_invoice", "invoice_no"}),
            Map.entry("ZY", new String[]{"biz_admission", "admission_no"}),
            Map.entry("YO", new String[]{"biz_medical_order", "order_no"}),
            Map.entry("JS", new String[]{"biz_settlement", "settlement_no"}),
            Map.entry("EX", new String[]{"biz_exam_apply", "apply_no"}),
            Map.entry("VAC", new String[]{"biz_vaccination", "vaccination_no"}),
            Map.entry("ID", new String[]{"biz_infectious_report", "report_no"}),
            Map.entry("PE", new String[]{"biz_physical_exam", "exam_no"})
    );

    private final StringRedisTemplate stringRedisTemplate;
    private final JdbcTemplate jdbcTemplate;

    private final AtomicLong localFallback = new AtomicLong(0);

    /**
     * 生成下一个业务单号。
     *
     * @param prefix 单号前缀，例如 REG / PR / CH / ZY
     */
    public String next(String prefix) {
        String day = LocalDate.now().format(DATE_FORMAT);
        long seq = increment(prefix, day);
        // 不再对 10000 取模：超过 9999 位时直接进位（变 5 位），取模会绕回重复号
        return prefix + day + String.format("%04d", seq);
    }

    private long increment(String prefix, String day) {
        String key = KEY_PREFIX + prefix + ":" + day;
        long dbMax = maxFromDb(prefix, day);
        try {
            Long value = stringRedisTemplate.opsForValue().increment(key);
            if (value != null && value > dbMax) {
                if (value == 1L) {
                    stringRedisTemplate.expire(key, KEY_TTL);
                }
                return value;
            }
            // Redis 计数器落后于数据库（重启/过期/清空），用库里最大号重新对齐后再自增
            stringRedisTemplate.opsForValue().set(key, String.valueOf(dbMax), KEY_TTL);
            Long realigned = stringRedisTemplate.opsForValue().increment(key);
            if (realigned != null) {
                return realigned;
            }
        } catch (Exception e) {
            log.warn("Redis 自增失败，使用本地流水号: {}", e.getMessage());
        }
        return Math.max(localFallback.incrementAndGet(), dbMax + 1);
    }

    /**
     * 查询当天已用的最大流水号，查不到返回 0。
     */
    private long maxFromDb(String prefix, String day) {
        String[] target = NO_COLUMNS.get(prefix);
        if (target == null) {
            return 0;
        }
        String head = prefix + day;
        try {
            String max = jdbcTemplate.queryForObject(
                    "SELECT MAX(`" + target[1] + "`) FROM `" + target[0] + "` WHERE `" + target[1] + "` LIKE ?",
                    String.class, head + "%");
            if (max == null || max.length() <= head.length()) {
                return 0;
            }
            return Long.parseLong(max.substring(head.length()));
        } catch (Exception e) {
            log.warn("查询 {} 当日最大单号失败，按 0 处理: {}", target[0], e.getMessage());
            return 0;
        }
    }
}
