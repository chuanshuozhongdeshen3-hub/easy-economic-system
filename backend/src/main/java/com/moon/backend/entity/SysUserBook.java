package com.moon.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_user_books")
@Getter
@Setter
public class SysUserBook {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "book_guid", nullable = false, unique = true)
    private String bookGuid;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

