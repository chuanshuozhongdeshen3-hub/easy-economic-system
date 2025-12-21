package com.moon.backend.repository;

import com.moon.backend.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {
    List<Account> findByBookGuid(String bookGuid);

    boolean existsByParentGuid(String parentGuid);

    Optional<Account> findFirstByBookGuidAndName(String bookGuid, String name);
}

