package com.moon.backend.service;

import com.moon.backend.dto.CustomerRequest;
import com.moon.backend.dto.EntryBatchRequest;
import com.moon.backend.dto.EntryItemRequest;
import com.moon.backend.dto.NameIdResponse;
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
