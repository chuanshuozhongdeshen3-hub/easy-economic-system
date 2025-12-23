package com.moon.backend.controller;

import com.moon.backend.dto.AccountNodeResponse;
import com.moon.backend.dto.ApiResponse;
import com.moon.backend.dto.CreateAccountRequest;
import com.moon.backend.dto.UpdateAccountRequest;
import com.moon.backend.dto.RelatedDocResponse;
import com.moon.backend.entity.Account;
import com.moon.backend.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<AccountNodeResponse>>> getTree(@RequestParam String bookGuid) {
        List<AccountNodeResponse> tree = accountService.getAccountTree(bookGuid);
        return ResponseEntity.ok(ApiResponse.ok("查询成功", tree));
    }

    @GetMapping("/{guid}/related")
    public ResponseEntity<ApiResponse<List<RelatedDocResponse>>> relatedDocs(@PathVariable String guid,
                                                                             @RequestParam String bookGuid,
                                                                             @RequestParam(defaultValue = "false") boolean includeChildren) {
        List<RelatedDocResponse> docs = accountService.listRelatedDocs(bookGuid, guid, includeChildren);
        return ResponseEntity.ok(ApiResponse.ok("查询成功", docs));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Account>> create(@Valid @RequestBody CreateAccountRequest request) {
        Account created = accountService.createAccount(request);
        return ResponseEntity.ok(ApiResponse.ok("创建成功", created));
    }

    @PutMapping("/{guid}")
    public ResponseEntity<ApiResponse<Account>> update(@PathVariable String guid, @Valid @RequestBody UpdateAccountRequest request) {
        request.setGuid(guid);
        Account updated = accountService.updateAccount(request);
        return ResponseEntity.ok(ApiResponse.ok("更新成功", updated));
    }

    @DeleteMapping("/{guid}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String guid) {
        accountService.deleteAccount(guid);
        return ResponseEntity.ok(ApiResponse.ok("删除成功", null));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }
}
