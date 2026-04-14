package com.holaho.intern.shared.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class AssignRolesRequest {
    private List<String> roleCodes;
}

