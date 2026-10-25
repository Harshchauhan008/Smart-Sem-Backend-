
package com.smartsem.smartsem.service;

import com.smartsem.smartsem.entity.Assignment;
import com.smartsem.smartsem.entity.Semester;
import com.smartsem.smartsem.entity.Subject;
import com.smartsem.smartsem.repository.AssignmentRepository;
import com.smartsem.smartsem.repository.SemesterRepository;
import com.smartsem.smartsem.repository.SubjectRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class AssignmentReminderScheduler {

    private final AssignmentRepository assignmentRepository;
    private final SubjectRepository subjectRepository;
    private final SemesterRepository semesterRepository;
    private final NotificationService notificationService;

    public AssignmentReminderScheduler(
            AssignmentRepository assignmentRepository,
            SubjectRepository subjectRepository,
            SemesterRepository semesterRepository,
            NotificationService notificationService) {

        this.assignmentRepository = assignmentRepository;
        this.subjectRepository = subjectRepository;
        this.semesterRepository = semesterRepository;
        this.notificationService = notificationService;
    }

    // Run every hour after a one-minute initial delay.
    @Scheduled(fixedRate = 3600000, initialDelay = 60000)
    public void checkUpcomingAssignments() {

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime reminderWindow = now.plusHours(24);

        List<Assignment> assignments =
                assignmentRepository.findByDeadlineBetweenAndStatusIn(
                        now,
                        reminderWindow,
                        List.of("PENDING", "IN_PROGRESS")
                );

        for (Assignment assignment : assignments) {

            Subject subject = subjectRepository
                    .findById(assignment.getSubjectId())
                    .orElse(null);

            if (subject == null) {
                continue;
            }

            Semester semester = semesterRepository
                    .findById(subject.getSemesterId())
                    .orElse(null);

            if (semester == null) {
                continue;
            }

            Long userId = semester.getUserId();
            Long assignmentId = assignment.getId();

            boolean alreadyExists =
                    notificationService.assignmentReminderExists(
                            userId,
                            assignmentId
                    );

            if (alreadyExists) {
                continue;
            }

            String title = "Assignment deadline approaching";
            String message = "Your assignment \""
                    + assignment.getTitle()
                    + "\" is due within the next 24 hours.";

            notificationService.createNotification(
                    userId,
                    title,
                    message,
                    "ASSIGNMENT_REMINDER",
                    assignmentId
            );
        }
    }

    public void triggerReminderCheck() {
    checkUpcomingAssignments();
    }

}
