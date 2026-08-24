package com.holaho.intern.shared.util;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

public final class WorkingDayCalculator {

    private WorkingDayCalculator() {
        // Utility class
    }

    /**
     * Calculates due date by adding N working days (skipping Saturday and Sunday).
     *
     * @param start       Start timestamp
     * @param workingDays Number of working days to add
     * @return Calculated due LocalDateTime
     */
    public static LocalDateTime calculateDueDate(LocalDateTime start, int workingDays) {
        if (start == null) {
            return null;
        }
        LocalDateTime current = start;
        int addedDays = 0;
        while (addedDays < workingDays) {
            current = current.plusDays(1);
            DayOfWeek day = current.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                addedDays++;
            }
        }
        return current;
    }
}
