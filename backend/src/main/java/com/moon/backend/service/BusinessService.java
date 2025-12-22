package com.moon.backend.service;

import com.moon.backend.dto.CustomerRequest;
import com.moon.backend.dto.EntryBatchRequest;
import com.moon.backend.dto.EntryItemRequest;
import com.moon.backend.dto.NameIdResponse;
import com.moon.backend.dto.NameStatusResponse;
import com.moon.backend.dto.PurchaseOrderRequest;
import com.moon.backend.dto.SalesInvoiceCreateRequest;
import com.moon.backend.dto.VendorRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public String createVendor(VendorRequest request) {
        String vendorGuid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO vendors (guid, book_guid, name, id, notes, tax_id, email, phone, addr, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                vendorGuid,
                request.getBookGuid(),
                request.getName(),
                request.getId(),
                request.getNotes(),
                request.getTaxId(),
                request.getEmail(),
                request.getPhone(),
                request.getAddr(),
                now,
                now
        );

        ensureOwner(request.getBookGuid(), "VENDOR", vendorGuid, null, null, request.getName());
        return vendorGuid;
    }

    @Transactional
    public String createCustomer(CustomerRequest request) {
        String customerGuid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO customers (guid, book_guid, name, id, notes, tax_id, email, phone, addr, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                customerGuid,
                request.getBookGuid(),
                request.getName(),
                request.getId(),
                request.getNotes(),
                request.getTaxId(),
                request.getEmail(),
                request.getPhone(),
                request.getAddr(),
                now,
                now
        );

        ensureOwner(request.getBookGuid(), "CUSTOMER", null, customerGuid, null, request.getName());
        return customerGuid;
    }

    @Transactional
    public String createPurchaseOrder(PurchaseOrderRequest request) {
        String ownerGuid = findOwnerGuidByVendor(request.getBookGuid(), request.getVendorGuid());
        String orderGuid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO orders (guid, book_guid, owner_guid, job_guid, order_type, id, date_opened, date_closed, notes, status) " +
                        "VALUES (?, ?, ?, ?, 'PURCHASE', ?, ?, NULL, ?, 'DRAFT')",
                orderGuid,
                request.getBookGuid(),
                ownerGuid,
                request.getJobGuid(),
                request.getOrderId(),
                now,
                request.getNotes()
        );
        return orderGuid;
    }

    @Transactional
    public String createSalesInvoice(SalesInvoiceCreateRequest request) {
        String ownerGuid = findOwnerGuidByCustomer(request.getBookGuid(), request.getCustomerGuid());
        String invoiceGuid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO invoices (guid, book_guid, owner_guid, job_guid, invoice_type, id, date_opened, date_posted, due_date, notes, status, post_txn_guid, lot_guid) " +
                        "VALUES (?, ?, ?, ?, 'SALES', ?, ?, NULL, NULL, ?, 'DRAFT', NULL, NULL)",
                invoiceGuid,
                request.getBookGuid(),
                ownerGuid,
                request.getJobGuid(),
                request.getInvoiceId(),
                now,
                request.getNotes()
        );
        return invoiceGuid;
    }

    public List<NameIdResponse> listVendors(String bookGuid) {
        return jdbcTemplate.query(
                "SELECT guid, name FROM vendors WHERE book_guid = ?",
                (rs, i) -> new NameIdResponse(rs.getString("guid"), rs.getString("name")),
                bookGuid
        );
    }

    public List<NameIdResponse> listCustomers(String bookGuid) {
        return jdbcTemplate.query(
                "SELECT guid, name FROM customers WHERE book_guid = ?",
                (rs, i) -> new NameIdResponse(rs.getString("guid"), rs.getString("name")),
                bookGuid
        );
    }

    public List<NameIdResponse> listEmployees(String bookGuid) {
        return jdbcTemplate.query(
                "SELECT guid, name FROM employees WHERE book_guid = ?",
                (rs, i) -> new NameIdResponse(rs.getString("guid"), rs.getString("name")),
                bookGuid
        );
    }

    public List<NameIdResponse> listJobs(String bookGuid) {
        return jdbcTemplate.query(
                "SELECT guid, name FROM jobs WHERE book_guid = ? AND active = 1",
                (rs, i) -> new NameIdResponse(rs.getString("guid"), rs.getString("name")),
                bookGuid
        );
    }

    public List<NameIdResponse> listPurchaseOrders(String bookGuid, String status) {
        StringBuilder sql = new StringBuilder("SELECT guid, id AS name FROM orders WHERE book_guid = ? AND order_type = 'PURCHASE'");
        List<Object> args = new java.util.ArrayList<>();
        args.add(bookGuid);
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
        return jdbcTemplate.query(
                sql.toString(),
                (rs, i) -> new NameIdResponse(rs.getString("guid"), rs.getString("name")),
                args.toArray()
        );
    }

    public List<NameIdResponse> listSalesInvoices(String bookGuid, String status) {
        StringBuilder sql = new StringBuilder("SELECT guid, id AS name FROM invoices WHERE book_guid = ? AND invoice_type = 'SALES'");
        List<Object> args = new java.util.ArrayList<>();
        args.add(bookGuid);
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
        return jdbcTemplate.query(
                sql.toString(),
                (rs, i) -> new NameIdResponse(rs.getString("guid"), rs.getString("name")),
                args.toArray()
        );
    }

    public List<NameIdResponse> listPurchaseInvoices(String bookGuid) {
        return jdbcTemplate.query(
                "SELECT guid, id AS name FROM invoices WHERE book_guid = ? AND invoice_type = 'PURCHASE'",
                (rs, i) -> new NameIdResponse(rs.getString("guid"), rs.getString("name")),
                bookGuid
        );
    }

    /** 详细列表：供应商 */
    public List<NameStatusResponse> listVendorDetails(String bookGuid) {
        return jdbcTemplate.query(
                "SELECT guid, name, notes, created_at FROM vendors WHERE book_guid = ? ORDER BY created_at DESC",
                (rs, i) -> new NameStatusResponse(
                        rs.getString("guid"),
                        rs.getString("name"),
                        null,
                        rs.getString("notes"),
                        rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                        null
                ),
                bookGuid
        );
    }

    /** 详细列表：客户 */
    public List<NameStatusResponse> listCustomerDetails(String bookGuid) {
        return jdbcTemplate.query(
                "SELECT guid, name, notes, created_at FROM customers WHERE book_guid = ? ORDER BY created_at DESC",
                (rs, i) -> new NameStatusResponse(
                        rs.getString("guid"),
                        rs.getString("name"),
                        null,
                        rs.getString("notes"),
                        rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                        null
                ),
                bookGuid
        );
    }

    /** 详细列表：员工 */
    public List<NameStatusResponse> listEmployeeDetails(String bookGuid) {
        return jdbcTemplate.query(
                "SELECT guid, name, notes, created_at FROM employees WHERE book_guid = ? ORDER BY created_at DESC",
                (rs, i) -> new NameStatusResponse(
                        rs.getString("guid"),
                        rs.getString("name"),
                        null,
                        rs.getString("notes"),
                        rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                        null
                ),
                bookGuid
        );
    }

    /** 详细列表：采购订单（含状态、付款状态） */
    public List<NameStatusResponse> listPurchaseOrdersDetail(String bookGuid) {
        String sql = """
                SELECT o.guid,
                       COALESCE(o.id, o.guid) AS name,
                       o.status,
                       o.notes,
                       o.date_opened,
                       COALESCE(SUM(CAST(e.price_num AS DECIMAL(18,4)) / NULLIF(e.price_denom,0)),0) AS amount,
                       EXISTS (
                         SELECT 1 FROM transactions t
                          WHERE t.book_guid = o.book_guid
                            AND t.source_guid = o.guid
                            AND t.source_type = 'PURCHASE_PAYMENT'
                       ) AS paid
                  FROM orders o
                  LEFT JOIN entries e ON e.order_guid = o.guid AND e.book_guid = o.book_guid
                 WHERE o.book_guid = ? AND o.order_type = 'PURCHASE'
                 GROUP BY o.guid, o.id, o.status, o.notes, o.date_opened, o.book_guid
                 ORDER BY o.date_opened DESC
                """;
        return jdbcTemplate.query(sql, (rs, i) -> {
            String status = rs.getString("status");
            boolean paid = rs.getBoolean("paid");
            String note = rs.getString("notes");
            if (paid) {
                status = status + " | 已支付";
            }
            return new NameStatusResponse(
                    rs.getString("guid"),
                    rs.getString("name"),
                    status,
                    note,
                    rs.getTimestamp("date_opened") != null ? rs.getTimestamp("date_opened").toLocalDateTime() : null,
                    rs.getBigDecimal("amount")
            );
        }, bookGuid);
    }

    /** 详细列表：销售发票（含状态、收款状态） */
    public List<NameStatusResponse> listSalesInvoicesDetail(String bookGuid) {
        String sql = """
                SELECT i.guid,
                       COALESCE(i.id, i.guid) AS name,
                       i.status,
                       i.notes,
                       i.date_opened,
                       COALESCE(SUM(CAST(e.price_num AS DECIMAL(18,4)) / NULLIF(e.price_denom,0)),0) AS amount,
                       EXISTS (
                         SELECT 1 FROM transactions t
                          WHERE t.book_guid = i.book_guid
                            AND t.source_guid = i.guid
                            AND t.source_type = 'SALES_RECEIPT'
                       ) AS collected
                  FROM invoices i
                  LEFT JOIN entries e ON e.invoice_guid = i.guid AND e.book_guid = i.book_guid
                 WHERE i.book_guid = ? AND i.invoice_type = 'SALES'
                 GROUP BY i.guid, i.id, i.status, i.notes, i.date_opened, i.book_guid
                 ORDER BY i.date_opened DESC
                """;
        return jdbcTemplate.query(sql, (rs, i) -> {
            String status = rs.getString("status");
            boolean collected = rs.getBoolean("collected");
            String note = rs.getString("notes");
            if (collected) {
                status = status + " | 已收款";
            }
            return new NameStatusResponse(
                    rs.getString("guid"),
                    rs.getString("name"),
                    status,
                    note,
                    rs.getTimestamp("date_opened") != null ? rs.getTimestamp("date_opened").toLocalDateTime() : null,
                    rs.getBigDecimal("amount")
            );
        }, bookGuid);
    }

    /** 详细列表：员工报销/差旅（含支付状态） */
    public List<NameStatusResponse> listEmployeeExpensesDetail(String bookGuid) {
        String sql = """
                SELECT t.guid,
                       COALESCE(t.num, t.guid) AS name,
                       t.doc_status AS status,
                       t.description AS notes,
                       t.post_date,
                       COALESCE(SUM(CAST(s.value_num AS DECIMAL(18,4)) / NULLIF(s.value_denom,0)),0) AS amount,
                       EXISTS (
                         SELECT 1 FROM transactions tp
                          WHERE tp.book_guid = t.book_guid
                            AND tp.source_guid = t.guid
                            AND tp.source_type = 'EMP_PAY'
                       ) AS paid
                  FROM transactions t
                  JOIN splits s ON s.tx_guid = t.guid
                  JOIN accounts a ON s.account_guid = a.guid AND a.account_type = 'EXPENSE'
                 WHERE t.book_guid = ?
                   AND t.source_type = 'EMP_EXPENSE'
                 GROUP BY t.guid, t.num, t.doc_status, t.description, t.post_date, t.book_guid
                 ORDER BY t.post_date DESC
                """;
        return jdbcTemplate.query(sql, (rs, i) -> {
            String status = rs.getString("status");
            if (rs.getBoolean("paid")) {
                status = status + " | 已支付";
            }
            return new NameStatusResponse(
                    rs.getString("guid"),
                    rs.getString("name"),
                    status,
                    rs.getString("notes"),
                    rs.getTimestamp("post_date") != null ? rs.getTimestamp("post_date").toLocalDateTime() : null,
                    rs.getBigDecimal("amount")
            );
        }, bookGuid);
    }

    @Transactional
    public void addOrderEntries(String orderGuid, EntryBatchRequest request) {
        LocalDateTime now = LocalDateTime.now();
        for (EntryItemRequest item : request.getItems()) {
            jdbcTemplate.update(
                    "INSERT INTO entries (guid, book_guid, invoice_guid, order_guid, job_guid, description, date, quantity_num, quantity_denom, price_num, price_denom, discount_num, discount_denom, account_guid, tax_table_guid, taxable, tax_included, tax_amount_num, tax_amount_denom, created_at, updated_at) " +
                            "VALUES (?, ?, NULL, ?, NULL, ?, ?, 1, 1, ?, 100, NULL, NULL, ?, NULL, ?, ?, NULL, NULL, ?, ?)",
                    UUID.randomUUID().toString(),
                    request.getBookGuid(),
                    orderGuid,
                    item.getDescription(),
                    now,
                    item.getAmountCent(),
                    item.getAccountGuid(),
                    taxableFlag(item),
                    taxIncludedFlag(item),
                    now,
                    now
            );
        }
    }

    @Transactional
    public void addInvoiceEntries(String invoiceGuid, EntryBatchRequest request) {
        LocalDateTime now = LocalDateTime.now();
        for (EntryItemRequest item : request.getItems()) {
            jdbcTemplate.update(
                    "INSERT INTO entries (guid, book_guid, invoice_guid, order_guid, job_guid, description, date, quantity_num, quantity_denom, price_num, price_denom, discount_num, discount_denom, account_guid, tax_table_guid, taxable, tax_included, tax_amount_num, tax_amount_denom, created_at, updated_at) " +
                            "VALUES (?, ?, ?, NULL, NULL, ?, ?, 1, 1, ?, 100, NULL, NULL, ?, NULL, ?, ?, NULL, NULL, ?, ?)",
                    UUID.randomUUID().toString(),
                    request.getBookGuid(),
                    invoiceGuid,
                    item.getDescription(),
                    now,
                    item.getAmountCent(),
                    item.getAccountGuid(),
                    taxableFlag(item),
                    taxIncludedFlag(item),
                    now,
                    now
            );
        }
    }

    private int taxableFlag(EntryItemRequest item) {
        Double rate = item.getTaxRatePercent();
        return (rate != null && rate > 0) ? 1 : 0;
    }

    private int taxIncludedFlag(EntryItemRequest item) {
        Boolean incl = item.getTaxIncluded();
        return Boolean.TRUE.equals(incl) ? 1 : 0;
    }

    private void ensureOwner(String bookGuid, String ownerType, String vendorGuid, String customerGuid, String employeeGuid, String name) {
        // 如果已有对应 owner 则跳过
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM owner WHERE book_guid = ? AND COALESCE(vendor_guid,'') = COALESCE(?, '') AND COALESCE(customer_guid,'') = COALESCE(?, '') AND COALESCE(employee_guid,'') = COALESCE(?, '')",
                Integer.class,
                bookGuid,
                vendorGuid,
                customerGuid,
                employeeGuid
        );
        if (exists != null && exists > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO owner (guid, book_guid, owner_type, customer_guid, vendor_guid, employee_guid, name) VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(),
                bookGuid,
                ownerType,
                customerGuid,
                vendorGuid,
                employeeGuid,
                name
        );
    }

    private String findOwnerGuidByVendor(String bookGuid, String vendorGuid) {
        String guid = jdbcTemplate.query(
                "SELECT guid FROM owner WHERE book_guid = ? AND vendor_guid = ? LIMIT 1",
                rs -> rs.next() ? rs.getString("guid") : null,
                bookGuid,
                vendorGuid
        );
        if (guid != null) {
            return guid;
        }
        throw new IllegalArgumentException("未找到供应商对应的往来对象，请先创建供应商");
    }

    private String findOwnerGuidByCustomer(String bookGuid, String customerGuid) {
        String guid = jdbcTemplate.query(
                "SELECT guid FROM owner WHERE book_guid = ? AND customer_guid = ? LIMIT 1",
                rs -> rs.next() ? rs.getString("guid") : null,
                bookGuid,
                customerGuid
        );
        if (guid != null) {
            return guid;
        }
        throw new IllegalArgumentException("未找到客户对应的往来对象，请先创建客户");
    }
}
