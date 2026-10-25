package com.xunfang.healthcare.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.util.StrUtil;
import com.xunfang.healthcare.common.BizException;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.PrescriptionStatus;
import com.xunfang.healthcare.dto.DispenseRequest;
import com.xunfang.healthcare.dto.StockRequest;
import com.xunfang.healthcare.entity.BizCharge;
import com.xunfang.healthcare.entity.BizDrug;
import com.xunfang.healthcare.entity.BizPrescription;
import com.xunfang.healthcare.entity.BizPrescriptionItem;
import com.xunfang.healthcare.entity.BizPrescriptionReturn;
import com.xunfang.healthcare.entity.BizRefund;
import com.xunfang.healthcare.entity.BizStock;
import com.xunfang.healthcare.entity.BizStockRecord;
import com.xunfang.healthcare.entity.BizWarehouse;
import com.xunfang.healthcare.mapper.BizChargeMapper;
import com.xunfang.healthcare.mapper.BizDrugMapper;
import com.xunfang.healthcare.mapper.BizPrescriptionItemMapper;
import com.xunfang.healthcare.mapper.BizPrescriptionMapper;
import com.xunfang.healthcare.mapper.BizPrescriptionReturnMapper;
import com.xunfang.healthcare.mapper.BizRefundMapper;
import com.xunfang.healthcare.mapper.BizStockMapper;
import com.xunfang.healthcare.mapper.BizStockRecordMapper;
import com.xunfang.healthcare.mapper.BizWarehouseMapper;
import com.xunfang.healthcare.security.SecurityUtils;
import com.xunfang.healthcare.util.BizNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 药品目录、库存与药房发药。
 */
@Service
@RequiredArgsConstructor
public class DrugService {

    private final BizDrugMapper bizDrugMapper;
    private final BizStockMapper bizStockMapper;
    private final BizStockRecordMapper bizStockRecordMapper;
    private final BizWarehouseMapper bizWarehouseMapper;
    private final BizPrescriptionMapper bizPrescriptionMapper;
    private final BizPrescriptionItemMapper bizPrescriptionItemMapper;
    private final BizChargeMapper bizChargeMapper;
    private final BizRefundMapper bizRefundMapper;
    private final BizPrescriptionReturnMapper bizPrescriptionReturnMapper;
    private final BizNoGenerator bizNoGenerator;

    /**
     * 药品分页。
     */
    public PageResult<BizDrug> pageDrugs(PageQuery query, String category, Integer isSpecial) {
        Page<BizDrug> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(bizDrugMapper.selectPage(page, Wrappers.<BizDrug>lambdaQuery()
                .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                        .like(BizDrug::getDrugName, query.getKeyword()).or()
                        .like(BizDrug::getDrugCode, query.getKeyword()).or()
                        .like(BizDrug::getGenericName, query.getKeyword()).or()
                        .like(BizDrug::getManufacturer, query.getKeyword()))
                .eq(StrUtil.isNotBlank(category), BizDrug::getCategory, category)
                .eq(isSpecial != null, BizDrug::getIsSpecial, isSpecial)
                .orderByAsc(BizDrug::getId)));
    }

    /**
     * 药品详情。
     */
    public BizDrug drugDetail(Long id) {
        BizDrug drug = bizDrugMapper.selectById(id);
        if (drug == null) {
            throw BizException.badRequest("药品不存在");
        }
        return drug;
    }

    /**
     * 保存药品。
     */
    public BizDrug saveDrug(BizDrug drug) {
        if (drug.getId() == null) {
            if (bizDrugMapper.selectCount(Wrappers.<BizDrug>lambdaQuery()
                    .eq(BizDrug::getDrugCode, drug.getDrugCode())) > 0) {
                throw BizException.badRequest("药品编码已存在");
            }
            if (drug.getStatus() == null) {
                drug.setStatus(1);
            }
            if (drug.getIsSpecial() == null) {
                drug.setIsSpecial(0);
            }
            bizDrugMapper.insert(drug);
        } else {
            bizDrugMapper.updateById(drug);
        }
        return drug;
    }

    /**
     * 删除药品。
     */
    public void deleteDrug(Long id) {
        drugDetail(id);
        bizDrugMapper.deleteById(id);
    }

    /**
     * 库房列表。
     */
    public List<BizWarehouse> listWarehouses() {
        return bizWarehouseMapper.selectList(Wrappers.<BizWarehouse>lambdaQuery()
                .eq(BizWarehouse::getStatus, 1)
                .orderByAsc(BizWarehouse::getId));
    }

    /**
     * 保存库房。
     */
    public BizWarehouse saveWarehouse(BizWarehouse warehouse) {
        if (warehouse.getId() == null) {
            if (warehouse.getStatus() == null) {
                warehouse.setStatus(1);
            }
            bizWarehouseMapper.insert(warehouse);
        } else {
            bizWarehouseMapper.updateById(warehouse);
        }
        return warehouse;
    }

    /**
     * 库存分页。
     */
    public PageResult<BizStock> pageStock(PageQuery query, Long drugId, Long warehouseId) {
        Page<BizStock> page = new Page<>(query.getPageNum(), query.getPageSize());
        PageResult<BizStock> result = PageResult.of(bizStockMapper.selectPage(page, Wrappers.<BizStock>lambdaQuery()
                .eq(drugId != null, BizStock::getDrugId, drugId)
                .eq(warehouseId != null, BizStock::getWarehouseId, warehouseId)
                .like(StrUtil.isNotBlank(query.getKeyword()), BizStock::getBatchNo, query.getKeyword())
                .orderByAsc(BizStock::getId)));
        fillDrugNames(result.getList());
        return result;
    }

    /**
     * 药品入库。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizStock stockIn(StockRequest request) {
        drugDetail(request.getDrugId());
        BizStock stock = findStock(request.getDrugId(), request.getBatchNo(), request.getWarehouseId());
        if (stock == null) {
            stock = new BizStock();
            stock.setDrugId(request.getDrugId());
            stock.setBatchNo(request.getBatchNo());
            stock.setWarehouseId(request.getWarehouseId());
            stock.setQuantity(0);
            stock.setLockQuantity(0);
            stock.setProduceDate(request.getProduceDate());
            stock.setExpireDate(request.getExpireDate());
            stock.setWarnNum(request.getWarnNum() == null ? 100 : request.getWarnNum());
        }
        stock.setQuantity(stock.getQuantity() + request.getQuantity());
        if (request.getExpireDate() != null) {
            stock.setExpireDate(request.getExpireDate());
        }
        if (request.getWarnNum() != null) {
            stock.setWarnNum(request.getWarnNum());
        }
        if (stock.getId() == null) {
            bizStockMapper.insert(stock);
        } else {
            bizStockMapper.updateById(stock);
        }
        saveStockRecord(stock, 1, request.getQuantity(), request.getBizNo());
        return stock;
    }

    /**
     * 药品出库。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizStock stockOut(StockRequest request) {
        BizStock stock = findStock(request.getDrugId(), request.getBatchNo(), request.getWarehouseId());
        if (stock == null) {
            throw BizException.badRequest("库存记录不存在");
        }
        if (stock.getQuantity() < request.getQuantity()) {
            throw BizException.badRequest("库存不足，当前库存 " + stock.getQuantity());
        }
        stock.setQuantity(stock.getQuantity() - request.getQuantity());
        bizStockMapper.updateById(stock);
        saveStockRecord(stock, 2, request.getQuantity(), request.getBizNo());
        return stock;
    }

    /**
     * 库存预警列表：库存低于预警数量。
     */
    public List<BizStock> warningList() {
        List<BizStock> list = bizStockMapper.selectList(Wrappers.<BizStock>lambdaQuery()
                .apply("quantity <= warn_num")
                .orderByAsc(BizStock::getQuantity));
        fillDrugNames(list);
        return list;
    }

    /**
     * 效期预警列表：90 天内到期或已过期。
     */
    public List<BizStock> expireList(int warnDays) {
        LocalDate deadline = LocalDate.now().plusDays(warnDays);
        List<BizStock> list = bizStockMapper.selectList(Wrappers.<BizStock>lambdaQuery()
                .isNotNull(BizStock::getExpireDate)
                .le(BizStock::getExpireDate, deadline)
                .orderByAsc(BizStock::getExpireDate));
        fillDrugNames(list);
        return list;
    }

    /**
     * 门诊发药。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizPrescription dispense(DispenseRequest request) {
        BizPrescription prescription = bizPrescriptionMapper.selectById(request.getPrescriptionId());
        if (prescription == null) {
            throw BizException.badRequest("处方不存在");
        }
        int status = prescription.getStatus() == null
                ? PrescriptionStatus.PENDING_CHARGE : prescription.getStatus();
        if (PrescriptionStatus.isUnpaid(status)) {
            throw BizException.badRequest("处方未收费，请先到收费处完成收费后再发药");
        }
        if (status != PrescriptionStatus.PAID) {
            throw BizException.badRequest("处方状态为"
                    + statusName(status) + "，不能发药");
        }
        List<BizPrescriptionItem> items = bizPrescriptionItemMapper.selectList(
                Wrappers.<BizPrescriptionItem>lambdaQuery()
                        .eq(BizPrescriptionItem::getPrescriptionId, request.getPrescriptionId()));
        if (request.getItemIds() != null && !request.getItemIds().isEmpty()) {
            items = items.stream().filter(item -> request.getItemIds().contains(item.getId())).toList();
        }
        if (items.isEmpty()) {
            throw BizException.badRequest("没有需要发药的明细");
        }

        for (BizPrescriptionItem item : items) {
            if (item.getDrugId() == null) {
                continue;
            }
            // 优先从「发药库房」出库；该库房没有这味药时（一个处方的药可能分存多个库房，
            // 例如门诊药房没有、只在中心库房有），退而求其次按效期从任意有货库房出。
            // 否则会因为个别药品库房不匹配直接抛错，导致整单发药失败、库存与流水都不动。
            BizStock stock = null;
            if (request.getWarehouseId() != null) {
                stock = bizStockMapper.selectOne(Wrappers.<BizStock>lambdaQuery()
                        .eq(BizStock::getDrugId, item.getDrugId())
                        .eq(BizStock::getWarehouseId, request.getWarehouseId())
                        .orderByAsc(BizStock::getExpireDate)
                        .last("limit 1"));
            }
            if (stock == null) {
                stock = bizStockMapper.selectOne(Wrappers.<BizStock>lambdaQuery()
                        .eq(BizStock::getDrugId, item.getDrugId())
                        .orderByAsc(BizStock::getExpireDate)
                        .last("limit 1"));
            }
            if (stock == null) {
                throw BizException.badRequest("药品「" + item.getDrugName() + "」无库存记录");
            }
            int quantity = item.getQuantity() == null ? 1 : item.getQuantity();
            if (stock.getQuantity() < quantity) {
                throw BizException.badRequest("药品「" + item.getDrugName() + "」库存不足（当前 " + stock.getQuantity() + "）");
            }
            stock.setQuantity(stock.getQuantity() - quantity);
            bizStockMapper.updateById(stock);
            saveStockRecord(stock, 2, quantity, prescription.getPrescNo());
        }

        BizPrescription update = new BizPrescription();
        update.setId(prescription.getId());
        update.setStatus(PrescriptionStatus.DISPENSED);
        bizPrescriptionMapper.updateById(update);
        return prescription;
    }

    /**
     * 退药：药品回库 + 按退回金额生成待审核退费单。
     * <p>实物退回与退款到账是两件事，这里只把退费单挂出来（status=0 待审核），
     * 真正的退钱由收费处在「退费管理」里审核放行（auditRefund → refund）。</p>
     * <p>整单退回时处方进入终态「已退」(4)；部分退回时处方仍保持「已发药」，
     * 因为病人手里还有药，只有金额被部分冲销。</p>
     *
     * @return prescription 处方、refundNo 退费单号、refundAmount 退费金额、fullReturn 是否整单退回
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> returnDrug(DispenseRequest request) {
        BizPrescription prescription = bizPrescriptionMapper.selectById(request.getPrescriptionId());
        if (prescription == null) {
            throw BizException.badRequest("处方不存在");
        }
        if (prescription.getStatus() == null || prescription.getStatus() != PrescriptionStatus.DISPENSED) {
            throw BizException.badRequest("只有已发药的处方可以退药");
        }
        List<BizPrescriptionItem> allItems = bizPrescriptionItemMapper.selectList(
                Wrappers.<BizPrescriptionItem>lambdaQuery()
                        .eq(BizPrescriptionItem::getPrescriptionId, request.getPrescriptionId()));
        if (allItems.isEmpty()) {
            throw BizException.badRequest("该处方没有药品明细，无需退药");
        }
        // 已经退过的明细：既用来防重复退药，也用来判断整张处方是否已全部退回
        Set<Long> returnedItemIds = bizPrescriptionReturnMapper.selectList(
                        Wrappers.<BizPrescriptionReturn>lambdaQuery()
                                .eq(BizPrescriptionReturn::getPrescriptionId, request.getPrescriptionId()))
                .stream()
                .map(BizPrescriptionReturn::getItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<BizPrescriptionItem> items;
        if (request.getItemIds() != null && !request.getItemIds().isEmpty()) {
            items = allItems.stream()
                    .filter(item -> request.getItemIds().contains(item.getId()))
                    .toList();
            if (items.isEmpty()) {
                throw BizException.badRequest("请选择要退回的药品明细");
            }
            List<String> dup = items.stream()
                    .filter(item -> returnedItemIds.contains(item.getId()))
                    .map(item -> item.getDrugName() == null ? String.valueOf(item.getId()) : item.getDrugName())
                    .toList();
            if (!dup.isEmpty()) {
                throw BizException.badRequest("以下药品已经退回过，请勿重复退药：" + String.join("、", dup));
            }
        } else {
            // 不指定明细视为退回剩余全部药品
            items = allItems.stream()
                    .filter(item -> !returnedItemIds.contains(item.getId()))
                    .toList();
            if (items.isEmpty()) {
                throw BizException.badRequest("该处方的药品已全部退回");
            }
        }
        boolean fullReturn = returnedItemIds.size() + items.size() >= allItems.size();

        for (BizPrescriptionItem item : items) {
            if (item.getDrugId() == null) {
                continue;
            }
            BizStock stock = findStock(item.getDrugId(), null, request.getWarehouseId());
            if (stock == null) {
                stock = new BizStock();
                stock.setDrugId(item.getDrugId());
                stock.setWarehouseId(request.getWarehouseId());
                stock.setQuantity(0);
                stock.setLockQuantity(0);
                stock.setWarnNum(100);
                bizStockMapper.insert(stock);
            }
            int quantity = item.getQuantity() == null ? 1 : item.getQuantity();
            stock.setQuantity(stock.getQuantity() + quantity);
            bizStockMapper.updateById(stock);
            saveStockRecord(stock, 4, quantity, prescription.getPrescNo());
        }

        BigDecimal returnAmount = items.stream()
                .map(BizPrescriptionItem::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BizRefund refund = createReturnRefund(prescription, returnAmount, fullReturn, request.getReason());

        for (BizPrescriptionItem item : items) {
            BizPrescriptionReturn record = new BizPrescriptionReturn();
            record.setPrescriptionId(prescription.getId());
            record.setItemId(item.getId());
            record.setDrugId(item.getDrugId());
            record.setQuantity(item.getQuantity() == null ? 1 : item.getQuantity());
            record.setAmount(item.getAmount());
            record.setRefundId(refund == null ? null : refund.getId());
            record.setWarehouseId(request.getWarehouseId());
            record.setReason(request.getReason());
            bizPrescriptionReturnMapper.insert(record);
        }

        if (fullReturn) {
            BizPrescription update = new BizPrescription();
            update.setId(prescription.getId());
            update.setStatus(PrescriptionStatus.REFUNDED);
            bizPrescriptionMapper.updateById(update);
        }

        Map<String, Object> data = new HashMap<>(8);
        data.put("prescription", prescription);
        data.put("fullReturn", fullReturn);
        data.put("returnAmount", returnAmount);
        data.put("refundNo", refund == null ? null : refund.getRefundNo());
        data.put("refundAmount", refund == null ? BigDecimal.ZERO : refund.getRefundAmount());
        return data;
    }

    /**
     * 退药后生成待审核退费单。
     * <p>金额取本次退回明细的小计合计，并按「收费单实收 − 已发起退费金额」截断，
     * 避免把同一笔钱退两次。退款方式记为 3 退药费，等待收费处在退费管理里审核放行。</p>
     */
    private BizRefund createReturnRefund(BizPrescription prescription, BigDecimal returnAmount,
                                         boolean fullReturn, String reason) {
        List<BizCharge> charges = bizChargeMapper.selectList(Wrappers.<BizCharge>lambdaQuery()
                .eq(BizCharge::getPrescriptionId, prescription.getId())
                .eq(BizCharge::getPayStatus, 1));
        if (charges.isEmpty()) {
            return null;
        }
        BizCharge charge = charges.get(0);
        BigDecimal already = bizRefundMapper.selectList(Wrappers.<BizRefund>lambdaQuery()
                        .eq(BizRefund::getChargeId, charge.getId())
                        .in(BizRefund::getStatus, 0, 1, 2))
                .stream()
                .map(BizRefund::getRefundAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = charge.getActualAmount() == null
                ? returnAmount : charge.getActualAmount().subtract(already);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal amount = returnAmount.compareTo(BigDecimal.ZERO) <= 0
                ? remaining : returnAmount.min(remaining);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BizRefund refund = new BizRefund();
        refund.setRefundNo(bizNoGenerator.next("RF"));
        refund.setChargeId(charge.getId());
        refund.setPatientId(prescription.getPatientId());
        refund.setRefundAmount(amount);
        refund.setRefundType(3);
        refund.setStatus(0);
        StringBuilder text = new StringBuilder("退药自动发起：处方 ").append(prescription.getPrescNo());
        if (!fullReturn) {
            text.append("（部分退药）");
        }
        if (StrUtil.isNotBlank(reason)) {
            text.append("，退药原因：").append(reason);
        }
        refund.setReason(text.toString());
        bizRefundMapper.insert(refund);
        return refund;
    }

    /**
     * 出入库流水分页。
     */
    public PageResult<BizStockRecord> pageStockRecords(PageQuery query, Long drugId) {
        Page<BizStockRecord> page = new Page<>(query.getPageNum(), query.getPageSize());
        PageResult<BizStockRecord> result = PageResult.of(bizStockRecordMapper.selectPage(page, Wrappers.<BizStockRecord>lambdaQuery()
                .eq(drugId != null, BizStockRecord::getDrugId, drugId)
                .orderByDesc(BizStockRecord::getCreateTime)));
        fillRecordDrugNames(result.getList());
        return result;
    }

    /**
     * 处方状态的中文名，用于拼装校验失败提示。
     */
    private String statusName(int status) {
        return switch (status) {
            case PrescriptionStatus.PENDING_CHARGE, PrescriptionStatus.LEGACY_AUDITED -> "待收费";
            case PrescriptionStatus.PAID -> "已收费";
            case PrescriptionStatus.DISPENSED -> "已发药";
            case PrescriptionStatus.REFUNDED -> "已退";
            case PrescriptionStatus.CANCELLED -> "已作废";
            default -> "未知";
        };
    }

    /**
     * 批量填充库存记录的药品名称，避免前端逐条反查。
     */
    private void fillDrugNames(List<BizStock> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, String> names = drugNames(list.stream()
                .map(BizStock::getDrugId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        list.forEach(stock -> stock.setDrugName(names.get(stock.getDrugId())));
    }

    /**
     * 批量填充出入库流水的药品名称。
     */
    private void fillRecordDrugNames(List<BizStockRecord> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, String> names = drugNames(list.stream()
                .map(BizStockRecord::getDrugId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        list.forEach(record -> record.setDrugName(names.get(record.getDrugId())));
    }

    /**
     * 按药品 ID 集合取药品名称。
     */
    private Map<Long, String> drugNames(List<Long> drugIds) {
        if (drugIds == null || drugIds.isEmpty()) {
            return Map.of();
        }
        List<BizDrug> drugs = bizDrugMapper.selectBatchIds(drugIds);
        Map<Long, String> names = new HashMap<>(drugs.size());
        drugs.forEach(drug -> names.put(drug.getId(), drug.getDrugName()));
        return names;
    }

    private BizStock findStock(Long drugId, String batchNo, Long warehouseId) {
        return bizStockMapper.selectOne(Wrappers.<BizStock>lambdaQuery()
                .eq(BizStock::getDrugId, drugId)
                .eq(StrUtil.isNotBlank(batchNo), BizStock::getBatchNo, batchNo)
                .eq(warehouseId != null, BizStock::getWarehouseId, warehouseId)
                .orderByAsc(BizStock::getExpireDate)
                .last("limit 1"));
    }

    private void saveStockRecord(BizStock stock, int recordType, int quantity, String bizNo) {
        BizStockRecord record = new BizStockRecord();
        record.setDrugId(stock.getDrugId());
        record.setRecordType(recordType);
        record.setQuantity(quantity);
        record.setBatchNo(stock.getBatchNo());
        record.setWarehouseId(stock.getWarehouseId());
        record.setOperatorId(SecurityUtils.getUserIdOrNull());
        record.setBizNo(bizNo);
        record.setUpdateTime(LocalDateTime.now());
        bizStockRecordMapper.insert(record);
    }
}
