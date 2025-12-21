package com.moon.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Getter
@Setter
public class Account {

    @Id
    private String guid;

    @Column(name = "book_guid", nullable = false)
    private String bookGuid;

    @Column(nullable = false)
    private String name;

    private String code;
    private String description;

    @Column(name = "account_type", nullable = false)
    private String accountType;

    @Column(name = "parent_guid")
    private String parentGuid;

    private Boolean hidden;
    private Boolean placeholder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

