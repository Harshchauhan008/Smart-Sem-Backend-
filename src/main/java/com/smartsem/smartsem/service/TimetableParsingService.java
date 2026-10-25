
package com.smartsem.smartsem.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TimetableParsingService {

    private static final Pattern TIME_RANGE = Pattern.compile(
            "(?i)\\b(\\d{1,2}:\\d{2})\\s*[-–to]+\\s*(\\d{1,2}:\\d{2})\\b"
    );

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("H:mm", Locale.ROOT);

    public List<String> findRecognizedSchedulePatterns(String text) {
        List<String> results = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return results;
        }

        String[] lines = text.split("\\R");

        for (String line : lines) {
            String normalized = line.trim();

            if (normalized.isEmpty()) {
                continue;
            }

            Matcher matcher = TIME_RANGE.matcher(normalized);

            while (matcher.find()) {
                try {
                    LocalTime start = LocalTime.parse(
                            matcher.group(1), TIME_FORMAT);
                    LocalTime end = LocalTime.parse(
                            matcher.group(2), TIME_FORMAT);

                    if (end.isAfter(start)) {
                        results.add(normalized);
                    }
                } catch (DateTimeParseException ignored) {
                    // Ignore time ranges that cannot be parsed safely.
                }
            }
        }

        return results;
    }

    public DayOfWeek parseDay(String text) {
        if (text == null) {
            return null;
        }

        String day = text.trim().toLowerCase(Locale.ROOT);

        return switch (day) {
            case "monday", "mon" -> DayOfWeek.MONDAY;
            case "tuesday", "tue", "tues" -> DayOfWeek.TUESDAY;
            case "wednesday", "wed" -> DayOfWeek.WEDNESDAY;
            case "thursday", "thu", "thur", "thurs" ->
                    DayOfWeek.THURSDAY;
            case "friday", "fri" -> DayOfWeek.FRIDAY;
            case "saturday", "sat" -> DayOfWeek.SATURDAY;
            case "sunday", "sun" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }
}
