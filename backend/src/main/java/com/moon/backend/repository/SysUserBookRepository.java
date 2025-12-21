package com.moon.backend.repository;

import com.moon.backend.entity.SysUserBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SysUserBookRepository extends JpaRepository<SysUserBook, Long> {
    Optional<SysUserBook> findByUserId(Long userId);
}

