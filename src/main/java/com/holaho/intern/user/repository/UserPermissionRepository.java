package com.holaho.intern.user.repository;

import com.holaho.intern.user.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {

    List<UserPermission> findByUserId(Long userId);

    void deleteByUserIdAndPermissionId(Long userId, Long permissionId);
}

