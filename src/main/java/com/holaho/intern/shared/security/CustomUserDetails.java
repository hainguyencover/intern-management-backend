package com.holaho.intern.shared.security;

import com.holaho.intern.shared.enums.UserStatus;


import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.entity.UserPermission;
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
    private final Long universityId;

    public CustomUserDetails(User user) {
        this(user, null);
    }

    public CustomUserDetails(User user, Long universityId) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.status = user.getStatus();
        this.universityId = universityId;

        var finalPermissions = new java.util.HashSet<String>();
        var roleAuthorities = new java.util.ArrayList<SimpleGrantedAuthority>();

        // 1. Load inherited permissions from roles
        if (user.getRoles() != null) {
            for (var role : user.getRoles()) {
                roleAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
                if (role.getPermissions() != null) {
                    for (var perm : role.getPermissions()) {
                        finalPermissions.add(perm.getCode());
                    }
                }
            }
        }

        // 2. Apply custom user-specific permission overrides (GRANT/REVOKE)
        if (user.getUserPermissions() != null) {
            for (var customPerm : user.getUserPermissions()) {
                String code = customPerm.getPermission().getCode();
                if (customPerm.getMode() == UserPermission.PermissionMode.GRANT) {
                    finalPermissions.add(code);
                } else if (customPerm.getMode() == UserPermission.PermissionMode.REVOKE) {
                    finalPermissions.remove(code);
                }
            }
        }

        // 3. Combine roles and active permissions into authorities list
        var authsList = new java.util.ArrayList<SimpleGrantedAuthority>(roleAuthorities);
        for (String permCode : finalPermissions) {
            authsList.add(new SimpleGrantedAuthority(permCode));
        }

        this.authorities = authsList;
    }

    public Long getId() {
        return id;
    }

    public Long getUniversityId() {
        return universityId;
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

