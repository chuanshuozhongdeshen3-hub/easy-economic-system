package com.moon.backend.service;

import com.moon.backend.dto.EmployeeExpensePostRequest;
import com.moon.backend.dto.EmployeePayRequest;
import com.moon.backend.dto.EmployeeRequest;
import com.moon.backend.entity.Account;
import com.moon.backend.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final JdbcTemplate jdbcTemplate;
    private final AccountRepository accountRepository;

    @Transactional
    public String createEmployee(EmployeeRequest request) {
        String employeeGuid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        String notes = mergeNotes(request.getNotes(), request.getCostCenter());

        jdbcTemplate.update(
                "INSERT INTO employees (guid, book_guid, name, id, notes, tax_id, email, phone, addr, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NULL, ?, ?, NULL, ?, ?)",
                employeeGuid,
                request.getBookGuid(),
                request.getName(),
                request.getId(),
                notes,
                request.getEmail(),
                request.getPhone(),
                now,
                now
        );

        ensureOwner(request.getBookGuid(), employeeGuid, request.getName());

        if (request.getProject() != null && !request.getProject().isBlank()) {
            String ownerGuid = findOwnerGuidByEmployee(request.getBookGuid(), employeeGuid);
            jdbcTemplate.update(
                    "INSERT INTO jobs (guid, book_guid, owner_guid, id, name, description, active, created_at, updated_at) " +
                            "VALUES (?, ?, ?, NULL, ?, ?, 1, ?, ?)",
                    UUID.randomUUID().toString(),
                    request.getBookGuid(),
                    ownerGuid,
                    request.getProject(),
                    "关联员工项目",
                    now,
                    now
            );
        }

        return employeeGuid;
    }

    @Transactional
    public void postExpense(EmployeeExpensePostRequest request) {
        if (request.getAmountCent() <= 0) {
            throw new IllegalArgumentException("金额必须大于 0");
        }
        String bookGuid = request.getBookGuid();
        Account expense = resolveDebitAccount(bookGuid, request.getDebitAccountName());
        Account payable = resolveByName(bookGuid, "应付职工薪酬")
                .orElseThrow(() -> new IllegalStateException("未找到“应付职工薪酬”科目"));

        LocalDateTime now = LocalDateTime.now();
        String txGuid = UUID.randomUUID().toString();

        jdbcTemplate.update(
                "INSERT INTO transactions (guid, book_guid, num, post_date, enter_date, description, doc_status, source_type, source_guid) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'POSTED', ?, ?)",
                txGuid,
                bookGuid,
                request.getExpenseNo(),
                request.getPostDate() != null ? request.getPostDate() : now,
                now,
                coalesce(request.getDescription(), "员工费用过账"),
                "EMP_EXPENSE",
                request.getEmployeeGuid()
        );

        insertSplit(txGuid, expense.getGuid(), request.getAmountCent(), request.getDescription());
        insertSplit(txGuid, payable.getGuid(), -request.getAmountCent(), request.getDescription());
    }

    @Transactional
    public void postPay(EmployeePayRequest request) {
        if (request.getAmountCent() <= 0) {
            throw new IllegalArgumentException("金额必须大于 0");
        }
        String bookGuid = request.getBookGuid();
        Account payable = resolveByName(bookGuid, "应付职工薪酬")
                .orElseThrow(() -> new IllegalStateException("未找到“应付职工薪酬”科目"));
        Account cash = resolveCashAccount(bookGuid, request.getCashAccountName());

        LocalDateTime now = LocalDateTime.now();
        String txGuid = UUID.randomUUID().toString();

        jdbcTemplate.update(
                "INSERT INTO transactions (guid, book_guid, num, post_date, enter_date, description, doc_status, source_type, source_guid) " +
                        "VALUES (?, ?, NULL, ?, ?, ?, 'POSTED', ?, ?)",
                txGuid,
                bookGuid,
                request.getPayDate() != null ? request.getPayDate() : now,
                now,
                coalesce(request.getDescription(), "员工付款过账"),
                "EMP_PAY",
                coalesce(request.getExpenseGuid(), request.getEmployeeGuid())
        );

        insertSplit(txGuid, payable.getGuid(), request.getAmountCent(), request.getDescription());
        insertSplit(txGuid, cash.getGuid(), -request.getAmountCent(), request.getDescription());
    }

    private void insertSplit(String txGuid, String accountGuid, long cents, String memo) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO splits (guid, tx_guid, account_guid, value_num, value_denom, quantity_num, quantity_denom, memo, action, reconcile_state, reconcile_date, lot_guid) " +
                        "VALUES (?, ?, ?, ?, 100, ?, 100, ?, NULL, 'N', NULL, NULL)",
                UUID.randomUUID().toString(),
                txGuid,
                accountGuid,
                cents,
                cents,
                memo
        );
    }

    private Optional<Account> resolveByName(String bookGuid, String name) {
        return accountRepository.findFirstByBookGuidAndName(bookGuid, name);
    }

    private Account resolveDebitAccount(String bookGuid, String name) {
        if (name != null && !name.isBlank()) {
            return resolveByName(bookGuid, name)
                    .orElseThrow(() -> new IllegalArgumentException("未找到科目：" + name));
        }
        return resolveByName(bookGuid, "管理费用")
                .or(() -> resolveByName(bookGuid, "销售费用"))
                .orElseThrow(() -> new IllegalStateException("未找到可用的费用科目"));
    }

    private Account resolveCashAccount(String bookGuid, String preferName) {
        if (preferName != null && !preferName.isBlank()) {
            return resolveByName(bookGuid, preferName)
                    .orElseThrow(() -> new IllegalArgumentException("未找到科目：" + preferName));
        }
        return resolveByName(bookGuid, "银行存款")
                .orElseThrow(() -> new IllegalStateException("未找到“银行存款”科目"));
    }

    public java.util.List<com.moon.backend.dto.NameIdResponse> listPostedExpenses(String bookGuid) {
        return jdbcTemplate.query(
                """
                SELECT guid, COALESCE(num, guid) AS name
                  FROM transactions
                 WHERE book_guid = ?
                   AND source_type = 'EMP_EXPENSE'
                   AND doc_status = 'POSTED'
                 ORDER BY post_date DESC
                """,
                (rs, i) -> new com.moon.backend.dto.NameIdResponse(rs.getString("guid"), rs.getString("name")),
                bookGuid
        );
    }

    private void ensureOwner(String bookGuid, String employeeGuid, String name) {
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM owner WHERE book_guid = ? AND employee_guid = ?",
                Integer.class,
                bookGuid,
                employeeGuid
        );
        if (exists != null && exists > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO owner (guid, book_guid, owner_type, customer_guid, vendor_guid, employee_guid, name) VALUES (?, ?, 'EMPLOYEE', NULL, NULL, ?, ?)",
                UUID.randomUUID().toString(),
                bookGuid,
                employeeGuid,
                name
        );
    }

    private String findOwnerGuidByEmployee(String bookGuid, String employeeGuid) {
        String guid = jdbcTemplate.query(
                "SELECT guid FROM owner WHERE book_guid = ? AND employee_guid = ? LIMIT 1",
                rs -> rs.next() ? rs.getString("guid") : null,
                bookGuid,
                employeeGuid
        );
        if (guid != null) {
            return guid;
        }
        throw new IllegalArgumentException("未找到员工对应的往来对象，请先创建员工");
    }

    private String mergeNotes(String notes, String costCenter) {
        if (costCenter == null || costCenter.isBlank()) {
            return notes;
        }
        String cc = "成本中心:" + costCenter;
        if (notes == null || notes.isBlank()) {
            return cc;
        }
        return notes + " | " + cc;
    }

    private String coalesce(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }
}
