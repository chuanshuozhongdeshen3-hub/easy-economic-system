package com.moon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class DashboardSummary {
    private BigDecimal cashTotal;
    private BigDecimal receivableOutstanding;
    private BigDecimal payableOutstanding;
    private BigDecimal receivedTotal;
    private BigDecimal paidTotal;
    /**
     * 收款进度（0-100）
     */
    private BigDecimal receivableProgress;
    /**
     * 付款进度（0-100）
     */
    private BigDecimal payableProgress;
    /**
     * 已开票数量（销售发票）
     */
    private Long salesInvoiceCount;
    /**
     * 回款单数量（销售收款）
     */
    private Long salesReceiptCount;
    /**
     * 采购发票数量（应付）
     */
    private Long purchaseInvoiceCount;
    /**
     * 采购支付数量（已付）
     */
    private Long purchasePaymentCount;
    /**
     * 待回款单据数（销售发票未结清）
     */
    private Long receivablePendingCount;
    /**
     * 待付款单据数（采购发票未结清）
     */
    private Long payablePendingCount;
    /**
     * 应交税费余额（取绝对值）
     */
    private BigDecimal taxDue;
}
