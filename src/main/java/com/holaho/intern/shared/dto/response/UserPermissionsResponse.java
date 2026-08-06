package com.holaho.intern.shared.dto.response;

import com.holaho.intern.user.entity.UserPermission.PermissionMode;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPermissionsResponse {
    private Long userId;
    private List<PermissionOverrideDetail> overrides;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PermissionOverrideDetail {
        private Long permissionId;
        private String permissionCode;
        private String permissionName;
        private PermissionMode mode;
    }
}
