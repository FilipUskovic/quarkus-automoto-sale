package com.carsoffer.common.utils;


import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class DateParser {

    public static LocalDate parseDate(String dateStr, String fieldName) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldName + " must be in the format 'YYYY-MM-DD'. Example: 2024-09-27.");
        }
    }

}

