package com.moon.backend.controller;

import com.moon.backend.dto.ApiResponse;
import com.moon.backend.dto.EmployeeExpensePostRequest;
import com.moon.backend.dto.EmployeePayRequest;
import com.moon.backend.dto.EmployeeRequest;
import com.moon.backend.dto.NameIdResponse;
import com.moon.backend.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> create(@Valid @RequestBody EmployeeRequest request) {
        String guid = employeeService.createEmployee(request);
        return ResponseEntity.ok(ApiResponse.ok("员工创建成功", guid));
    }

    @PostMapping("/expense/post")
    public ResponseEntity<ApiResponse<Void>> postExpense(@Valid @RequestBody EmployeeExpensePostRequest request) {
        employeeService.postExpense(request);
        return ResponseEntity.ok(ApiResponse.ok("员工费用过账成功", null));
    }

    @PostMapping("/pay/post")
    public ResponseEntity<ApiResponse<Void>> postPay(@Valid @RequestBody EmployeePayRequest request) {
        employeeService.postPay(request);
        return ResponseEntity.ok(ApiResponse.ok("员工付款过账成功", null));
    }

    @GetMapping("/expenses")
    public ResponseEntity<ApiResponse<java.util.List<NameIdResponse>>> listPostedExpenses(@RequestParam String bookGuid) {
        return ResponseEntity.ok(ApiResponse.ok("查询成功", employeeService.listPostedExpenses(bookGuid)));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }
}
