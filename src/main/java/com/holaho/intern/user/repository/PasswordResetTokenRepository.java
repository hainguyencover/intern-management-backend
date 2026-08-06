package com.holaho.intern.user.repository;

import com.holaho.intern.user.entity.PasswordResetToken;
import com.holaho.intern.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUser(User user);
    void deleteByToken(String token);
    void deleteByUser(User user);
}
