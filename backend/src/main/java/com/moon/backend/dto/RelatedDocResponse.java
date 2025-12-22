package com.moon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelatedDocResponse {
    private String docType;
    private String docId;
    private LocalDateTime docDate;
    private String description;
}
