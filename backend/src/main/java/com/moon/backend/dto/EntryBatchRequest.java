package com.moon.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class EntryBatchRequest {
    @NotBlank
    private String bookGuid;

    @NotEmpty
    @Valid
    private List<EntryItemRequest> items;
}
