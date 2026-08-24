package com.holaho.intern.integration.adapter;

import com.holaho.intern.integration.entity.IntegrationConnection;
import com.holaho.intern.integration.entity.IntegrationCredential;

import java.util.List;

public interface AttendanceAdapter {

    List<AttendanceEventDto> fetchEvents(IntegrationConnection connection, List<IntegrationCredential> credentials);

    boolean testConnection(IntegrationConnection connection, List<IntegrationCredential> credentials);

    boolean validateWebhookSignature(String payload, String signature, String secret);
}
