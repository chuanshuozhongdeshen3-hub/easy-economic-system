package com.moon.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "books")
@Getter
@Setter
public class Book {

    @Id
    private String guid;

    @Column(nullable = false)
    private String name;

    private String size;

    @Column(name = "registered_capital_num")
    private Long registeredCapitalNum;

    @Column(name = "registered_capital_denom")
    private Long registeredCapitalDenom;

    @Column(name = "root_account_guid", nullable = false)
    private String rootAccountGuid;

    @Column(name = "fiscal_year_start_month")
    private Integer fiscalYearStartMonth;

    @Column(name = "fiscal_year_start_day")
    private Integer fiscalYearStartDay;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

