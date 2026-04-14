package com.holaho.intern.shared.security;

import com.holaho.intern.shared.enums.UserStatus;


import com.holaho.intern.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final com.holaho.intern.shared.enums.UserStatus status;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.status = user.getStatus();
        this.authorities = user.getRoles().stream()
                .flatMap(role -> {
                    var auths = new java.util.ArrayList<SimpleGrantedAuthority>();
                    auths.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
                    role.getPermissions().forEach(p -> auths.add(new SimpleGrantedAuthority(p.getCode())));
                    return auths.stream();
                })
                .toList();
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != com.holaho.intern.shared.enums.UserStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == com.holaho.intern.shared.enums.UserStatus.ACTIVE;
    }
}

