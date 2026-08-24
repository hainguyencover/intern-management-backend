package com.holaho.intern.auth.service;

import com.holaho.intern.user.entity.User;

public interface EmailVerificationService {
    String createVerificationToken(User user);
    void verifyEmail(String token);
    void resendVerification(String email);
}
