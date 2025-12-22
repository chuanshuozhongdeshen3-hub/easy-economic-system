package com.moon.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmployeeRequest {
    @NotBlank
    private String bookGuid;

    @NotBlank
    private String name;

    private String id;
    private String email;
    private String phone;
    private String notes;

    /**
     * 成本中心（可选），暂存于备注
     */
    private String costCenter;

    /**
     * 关联项目名称（可选），若提供会创建一个 job 记录
     */
    private String project;
}
