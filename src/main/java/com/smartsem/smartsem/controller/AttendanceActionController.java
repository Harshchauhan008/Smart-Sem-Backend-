
package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.entity.AttendanceRecord;
import com.smartsem.smartsem.service.AttendanceActionService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AttendanceActionController {

    private final AttendanceActionService attendanceActionService;

    public AttendanceActionController(
            AttendanceActionService attendanceActionService) {
        this.attendanceActionService = attendanceActionService;
    }

    @PostMapping("/process-attendance")
    public ResponseEntity<?> processAttendance(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

                System.out.println("PROCESS ATTENDANCE CONTROLLER REACHED");

        String message = request.get("message");

        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Message is required."));
        }

        AttendanceRecord record =
                attendanceActionService.markAttendanceFromMessage(
                        message, authentication);

        return ResponseEntity.ok(Map.of(
                "message", "Attendance marked successfully.",
                "subjectId", record.getSubjectId(),
                "classDate", record.getClassDate().toString(),
                "status", record.getStatus()
        ));
    }
}
