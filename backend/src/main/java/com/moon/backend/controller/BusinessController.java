package com.moon.backend.controller;

import com.moon.backend.dto.ApiResponse;
import com.moon.backend.dto.CustomerRequest;
import com.moon.backend.dto.EntryBatchRequest;
import com.moon.backend.dto.NameIdResponse;
import com.moon.backend.dto.NameStatusResponse;
import com.moon.backend.dto.PurchaseOrderRequest;
import com.moon.backend.dto.SalesInvoiceCreateRequest;
import com.moon.backend.dto.VendorRequest;
import com.moon.backend.service.BusinessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    @PostMapping("/vendors")
    public ResponseEntity<ApiResponse<String>> createVendor(@Valid @RequestBody VendorRequest request) {
        String guid = businessService.createVendor(request);
        return ResponseEntity.ok(ApiResponse.ok("供应商创建成功", guid));
    }

    @PostMapping("/customers")
    public ResponseEntity<ApiResponse<String>> createCustomer(@Valid @RequestBody CustomerRequest request) {
        String guid = businessService.createCustomer(request);
        return ResponseEntity.ok(ApiResponse.ok("客户创建成功", guid));
    }

    @GetMapping("/vendors")
    public ResponseEntity<ApiResponse<java.util.List<NameIdResponse>>> listVendors(@RequestParam String bookGuid) {
        return ResponseEntity.ok(ApiResponse.ok("查询成功", businessService.listVendors(bookGuid)));
    }

    @GetMapping("/vendors/detail")
    public ResponseEntity<ApiResponse<java.util.List<NameStatusResponse>>> listVendorsDetail(@RequestParam String bookGuid) {
        return ResponseEntity.ok(ApiResponse.ok("查询成功", businessService.listVendorDetails(bookGuid)));
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<java.util.List<NameIdResponse>>> listCustomers(@RequestParam String bookGuid) {
        return ResponseEntity.ok(ApiResponse.ok("查询成功", businessService.listCustomers(bookGuid)));
    }

    @GetMapping("/customers/detail")
    public ResponseEntity<ApiResponse<java.util.List<NameStatusResponse>>> listCustomersDetail(@RequestParam String bookGuid) {
        return ResponseEntity.ok(ApiResponse.ok("查询成功", businessService.listCustomerDetails(bookGuid)));
    }

    @GetMapping("/employees")
    public ResponseEntity<ApiResponse<java.util.List<NameIdResponse>>> listEmployees(@RequestParam String bookGuid) {
        return ResponseEntity.ok(ApiResponse.ok("查询成功", businessService.listEmployees(bookGuid)));
    }

    @GetMapping("/employees/detail")
    public ResponseEntity<ApiResponse<java.util.List<NameStatusResponse>>> listEmployeesDetail(@RequestParam String bookGuid) {
        return ResponseEntity.ok(ApiResponse.ok("查询成功", businessService.listEmployeeDetails(bookGuid)));
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<java.util.List<NameIdResponse>>> listJobs(@RequestParam String bookGuid) {
        return ResponseEntity.ok(ApiResponse.ok("查询成功", businessService.listJobs(bookGuid)));
    }

    @GetMapping("/purchase/orders")
    public ResponseEntity<ApiResponse<java.util.List<NameIdResponse>>> listPurchaseOrders(@RequestParam String bookGuid,
                                                                                          @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok("查询成功", businessService.listPurchaseOrders(bookGuid, status)));
    }

    @GetMapping("/purchase/orders/detail")
    public ResponseEntity<ApiResponse<java.util.List<NameStatusResponse>>> listPurchaseOrdersDetail(@RequestParam String bookGuid) {
        return ResponseEntity.ok(ApiResponse.ok("查询成功", businessService.listPurchaseOrdersDetail(bookGuid)));
    }

    @GetMapping("/sales/invoices")
    public ResponseEntity<ApiResponse<java.util.List<NameIdResponse>>> listSalesInvoices(@RequestParam String bookGuid,
                                                                                         @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok("查询成功", businessService.listSalesInvoices(bookGuid, status)));
    }

    @GetMapping("/sales/invoices/detail")
    public ResponseEntity<ApiResponse<java.util.List<NameStatusResponse>>> listSalesInvoicesDetail(@RequestParam String bookGuid) {
        return ResponseEntity.ok(ApiResponse.ok("查询成功", businessService.listSalesInvoicesDetail(bookGuid)));
    }

    @GetMapping("/purchase/invoices")
    public ResponseEntity<ApiResponse<java.util.List<NameIdResponse>>> listPurchaseInvoices(@RequestParam String bookGuid) {
        return ResponseEntity.ok(ApiResponse.ok("查询成功", businessService.listPurchaseInvoices(bookGuid)));
    }

    @PostMapping("/purchase/orders")
    public ResponseEntity<ApiResponse<String>> createPurchaseOrder(@Valid @RequestBody PurchaseOrderRequest request) {
        String guid = businessService.createPurchaseOrder(request);
        return ResponseEntity.ok(ApiResponse.ok("采购订单创建成功", guid));
    }

    @PostMapping("/sales/invoices")
    public ResponseEntity<ApiResponse<String>> createSalesInvoice(@Valid @RequestBody SalesInvoiceCreateRequest request) {
        String guid = businessService.createSalesInvoice(request);
        return ResponseEntity.ok(ApiResponse.ok("销售发票创建成功", guid));
    }

    @PostMapping("/purchase/orders/{orderGuid}/entries")
    public ResponseEntity<ApiResponse<Void>> addOrderEntries(@PathVariable String orderGuid, @Valid @RequestBody EntryBatchRequest request) {
        businessService.addOrderEntries(orderGuid, request);
        return ResponseEntity.ok(ApiResponse.ok("订单行添加成功", null));
    }

    @PostMapping("/sales/invoices/{invoiceGuid}/entries")
    public ResponseEntity<ApiResponse<Void>> addInvoiceEntries(@PathVariable String invoiceGuid, @Valid @RequestBody EntryBatchRequest request) {
        businessService.addInvoiceEntries(invoiceGuid, request);
        return ResponseEntity.ok(ApiResponse.ok("发票行添加成功", null));
    }

    @GetMapping("/employee/expenses/detail")
    public ResponseEntity<ApiResponse<java.util.List<NameStatusResponse>>> listEmployeeExpensesDetail(@RequestParam String bookGuid) {
        return ResponseEntity.ok(ApiResponse.ok("查询成功", businessService.listEmployeeExpensesDetail(bookGuid)));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }
}
