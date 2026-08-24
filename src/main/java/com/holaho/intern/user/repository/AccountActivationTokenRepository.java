package com.holaho.intern.user.repository;

import com.holaho.intern.user.entity.AccountActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountActivationTokenRepository extends JpaRepository<AccountActivationToken, Long> {

    Optional<AccountActivationToken> findByTokenHash(String tokenHash);

    void deleteByUserId(Long userId);
}
