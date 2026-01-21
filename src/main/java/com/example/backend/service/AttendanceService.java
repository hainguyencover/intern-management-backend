package com.example.backend.service;

import com.example.backend.dto.request.CheckOutRequest;
import com.example.backend.dto.response.AttendanceResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AttendanceService {
    AttendanceResponse checkIn();
    AttendanceResponse checkOut(CheckOutRequest req);
    AttendanceResponse today();
    List<AttendanceResponse> history(int month, int year);
}
