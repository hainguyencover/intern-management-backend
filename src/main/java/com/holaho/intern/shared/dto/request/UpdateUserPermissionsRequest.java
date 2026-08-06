package com.holaho.intern.shared.dto.request;

import com.holaho.intern.user.entity.UserPermission.PermissionMode;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserPermissionsRequest {

    private List<PermissionOverrideDto> overrides;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PermissionOverrideDto {
        private Long permissionId;
        private PermissionMode mode;
    }
}
