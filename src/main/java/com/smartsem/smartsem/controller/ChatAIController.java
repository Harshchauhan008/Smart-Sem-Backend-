package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.dto.ChatRequest;
import com.smartsem.smartsem.dto.ChatResponse;
import com.smartsem.smartsem.entity.AcademicDocument;
import com.smartsem.smartsem.entity.Assignment;
import com.smartsem.smartsem.entity.AssignmentSubmission;
import com.smartsem.smartsem.entity.ChatConversation;
import com.smartsem.smartsem.entity.ChatMessage;
import com.smartsem.smartsem.entity.StudyMaterialChunk;
import com.smartsem.smartsem.entity.Subject;
import com.smartsem.smartsem.entity.TimetableEntry;
import com.smartsem.smartsem.entity.User;
import com.smartsem.smartsem.repository.AssignmentRepository;
import com.smartsem.smartsem.repository.AssignmentSubmissionRepository;
import com.smartsem.smartsem.repository.ChatConversationRepository;
import com.smartsem.smartsem.repository.SemesterRepository;
import com.smartsem.smartsem.repository.StudyMaterialChunkRepository;
import com.smartsem.smartsem.repository.SubjectRepository;
import com.smartsem.smartsem.repository.TimetableEntryRepository;
import com.smartsem.smartsem.repository.UserRepository;
import com.smartsem.smartsem.service.AcademicDocumentService;
import com.smartsem.smartsem.service.AttendanceRecordService;
import com.smartsem.smartsem.service.ChatMessageService;
import com.smartsem.smartsem.service.GeminiService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conversations/{conversationId}/ai")
public class ChatAIController {

    private static final int MAX_DOCUMENT_CONTEXT_CHARS = 12000;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final Duration PENDING_TTL = Duration.ofMinutes(10);

    private static final String QUOTA_MESSAGE =
            "I've reached my AI usage limit for now. Please try again in a little while.";

    // Tells Gemini what this chat can and cannot do, so it does not
    // invent features such as reminders.
    private static final String CAPABILITY_RULES = """
            What SmartSem can do from this chat:
            - Answer general academic questions.
            - Answer questions about the student's subjects, timetable and
              assignments, using the verified data provided.
            - Record attendance (it always asks the student to confirm first).
            - Mark an existing assignment as submitted (it also asks to confirm first).
            - Add a new assignment with a deadline to a subject (it also asks
              to confirm first).

            What SmartSem CANNOT do from this chat:
            - Set reminders or notifications.
            - Edit or delete assignments, deadlines, subjects or timetable entries.
            - Create subjects or timetable entries.
            - Change any other data.

            If the student asks for something outside the first list, say plainly
            that it can't be done from the chat yet, and suggest the relevant
            screen in the app (for example the Timetable or Subjects screen).
            Never suggest example commands for things SmartSem cannot do.
            """;

    // Status values stored in assignments.status.
    private static final String SUBMITTED_STATUS = "SUBMITTED";
    private static final String PENDING_STATUS = "PENDING";

    private static final List<Pattern> ATTENDANCE_PATTERNS = List.of(
            Pattern.compile("\\b(mark|record|update|log)\\s+(my\\s+)?attendance\\b"),
            Pattern.compile("\\bmark\\s+me\\s+(as\\s+)?(present|absent)\\b"),
            Pattern.compile("\\bi\\s+(was|were)\\s+(present|absent)\\b"),
            Pattern.compile("\\bi\\s+(attended|missed|bunked|skipped)\\b")
    );

    // "make it submitted", "mark X as submitted", "I submitted X" ...
    // (apostrophes are removed before matching, so "I've" becomes "ive")
    private static final List<Pattern> SUBMIT_PATTERNS = List.of(
            Pattern.compile("\\b(make|mark|set|update|change|put)\\b.*\\b(submitted|completed|done|finished)\\b"),
            Pattern.compile("\\bi(ve|\\s+have)?\\s+(already\\s+)?(submitted|completed|finished)\\b")
    );

    // "add an assignment ...", "create a new assignment ...", "new homework ..."
    private static final List<Pattern> ADD_ASSIGNMENT_PATTERNS = List.of(
            Pattern.compile("\\b(add|create)\\b.*\\b(assignments?|homework)\\b"),
            Pattern.compile("\\bnew\\s+(assignment|homework)\\b"),
            Pattern.compile("\\bmake\\s+(a|an)\\s+(new\\s+)?(assignment|homework)\\b")
    );

    private static final Pattern ASSIGNMENT_NOUNS = Pattern.compile(
            "\\b(assignments?|homework)\\b");

    private static final Pattern REFERS_TO_IT = Pattern.compile(
            "\\b(it|this|that)\\b");

    // Words that mean the question is about assignments or submissions.
    private static final Pattern ASSIGNMENT_WORDS = Pattern.compile(
            "\\b(assignments?|homework|submit(ted|ting)?|submissions?|deadlines?|due|pending|overdue)\\b");

    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)
                    .withZone(IST);

    private static final Set<String> CANCEL_WORDS = Set.of(
            "cancel", "never mind", "nevermind", "forget it", "stop");

    // Replies to the "please confirm" question.
    private static final Set<String> YES_WORDS = Set.of(
            "yes", "y", "yeah", "yep", "yup", "confirm", "confirmed",
            "ok", "okay", "sure", "correct", "right", "go ahead",
            "do it", "mark it");

    private static final Set<String> NO_WORDS = Set.of(
            "no", "n", "nope", "nah", "cancel", "never mind",
            "nevermind", "forget it", "stop", "wrong");

    private static final Pattern PRESENT_CONFIRM = Pattern.compile(
            "^(yes )?(i )?(have )?(attended|was present|am present)"
                    + "( it| that| this| the class)?$|^present$");

    private static final Pattern ABSENT_CONFIRM = Pattern.compile(
            "^(yes )?(i )?(was |am )?(absent|missed|skipped)"
                    + "( it| that| this| the class)?$|^absent$");

    /** An attendance request that is waiting for more details from the student. */
    private record PendingAttendance(String text, Instant createdAt) {}

    /** A fully-resolved attendance record waiting for the student's yes/no. */
    private record PendingConfirmation(
            Long subjectId,
            String subjectName,
            Long timetableEntryId,
            LocalDate classDate,
            String timeRange,
            String status,
            Instant createdAt) {}

    /** An assignment status change waiting for the student's yes/no. */
    private record PendingSubmit(
            Long assignmentId,
            String title,
            String subjectName,
            Instant createdAt) {}

    /** An "add assignment" request that is waiting for more details. */
    private record PendingAddDraft(String text, Instant createdAt) {}

    /** A fully-resolved new assignment waiting for the student's yes/no. */
    private record PendingAdd(
            Long subjectId,
            String subjectName,
            String title,
            OffsetDateTime deadline,
            Instant createdAt) {}

    /** Result of handling an "add assignment" message. */
    private record AddOutcome(
            String reply,
            boolean needsMoreInfo,
            PendingAdd confirmation) {}

    /** Reply for a submit request, plus a pending change if one needs confirming. */
    private record SubmitOutcome(String reply, PendingSubmit pending) {}

    /** An assignment together with the name of its subject. */
    private record AssignmentRef(Assignment assignment, String subjectName) {}

    /**
     * Result of handling attendance: the reply, whether more info is needed,
     * and (optionally) a record that now needs the student's confirmation.
     */
    private record AttendanceOutcome(
            String reply,
            boolean needsMoreInfo,
            PendingConfirmation confirmation) {}

    private final Map<Long, PendingAttendance> pendingAttendance =
            new ConcurrentHashMap<>();

    private final Map<Long, PendingConfirmation> pendingConfirmations =
            new ConcurrentHashMap<>();

    private final Map<Long, PendingSubmit> pendingSubmits =
            new ConcurrentHashMap<>();

    private final Map<Long, PendingAddDraft> pendingAddDrafts =
            new ConcurrentHashMap<>();

    private final Map<Long, PendingAdd> pendingAdds =
            new ConcurrentHashMap<>();

    // The assignment most recently discussed in each conversation,
    // so "make it submitted" knows what "it" means.
    private final Map<Long, Long> lastAssignmentByConversation =
            new ConcurrentHashMap<>();

    private final ChatConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final SemesterRepository semesterRepository;
    private final AcademicDocumentService documentService;
    private final ChatMessageService chatMessageService;
    private final GeminiService geminiService;
    private final StudyMaterialChunkRepository chunkRepository;
    private final AttendanceRecordService attendanceRecordService;
    private final SubjectRepository subjectRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final ObjectMapper objectMapper;

    public ChatAIController(
            ChatConversationRepository conversationRepository,
            UserRepository userRepository,
            SemesterRepository semesterRepository,
            AcademicDocumentService documentService,
            ChatMessageService chatMessageService,
            GeminiService geminiService,
            StudyMaterialChunkRepository chunkRepository,
            AttendanceRecordService attendanceRecordService,
            SubjectRepository subjectRepository,
            TimetableEntryRepository timetableEntryRepository,
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository assignmentSubmissionRepository,
            ObjectMapper objectMapper) {

        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.semesterRepository = semesterRepository;
        this.documentService = documentService;
        this.chatMessageService = chatMessageService;
        this.geminiService = geminiService;
        this.chunkRepository = chunkRepository;
        this.attendanceRecordService = attendanceRecordService;
        this.subjectRepository = subjectRepository;
        this.timetableEntryRepository = timetableEntryRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentSubmissionRepository = assignmentSubmissionRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/chat")
    public ChatResponse chat(
            @PathVariable Long conversationId,
            @RequestBody ChatRequest request,
            Authentication authentication) {

        if (authentication == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"));

        ChatConversation conversation =
                conversationRepository.findById(conversationId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Conversation not found"));

        if (!conversation.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot access this conversation");
        }

        if (request.getMessageText() == null
                || request.getMessageText().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message text is required");
        }

        String question = request.getMessageText().trim();
        String intent = request.getIntent();

        ChatMessage userMessage = new ChatMessage();
        userMessage.setConversationId(conversationId);
        userMessage.setRole("USER");
        userMessage.setMessageText(question);
        userMessage.setIntent(intent);

        userMessage = chatMessageService.saveMessage(userMessage);

        // ---- Step 2 of "add assignment": the student answers the confirmation ----
        PendingAdd pendingAdd = getPendingAdd(conversationId);

        if (pendingAdd != null) {
            if (isRejection(question)) {
                pendingAdds.remove(conversationId);

                return saveAssistantReply(
                        conversation,
                        conversationId,
                        userMessage,
                        question,
                        "Okay, I won't add **" + pendingAdd.title() + "**.",
                        intent);
            }

            if (isYes(question)) {
                pendingAdds.remove(conversationId);

                return saveAssistantReply(
                        conversation,
                        conversationId,
                        userMessage,
                        question,
                        recordConfirmedAddition(conversationId, pendingAdd),
                        intent);
            }

            // Not a yes/no: drop the confirmation and handle the message normally.
            pendingAdds.remove(conversationId);
        }

        // ---- Step 2 of an assignment change: the student answers the confirmation ----
        PendingSubmit pendingSubmit = getPendingSubmit(conversationId);

        if (pendingSubmit != null) {
            if (isRejection(question)) {
                pendingSubmits.remove(conversationId);

                return saveAssistantReply(
                        conversation,
                        conversationId,
                        userMessage,
                        question,
                        "Okay, I won't change the status of "
                                + pendingSubmit.title() + ".",
                        intent);
            }

            if (isYes(question)) {
                pendingSubmits.remove(conversationId);

                return saveAssistantReply(
                        conversation,
                        conversationId,
                        userMessage,
                        question,
                        recordConfirmedSubmission(pendingSubmit),
                        intent);
            }

            // Not a yes/no: drop the confirmation and handle the message normally.
            pendingSubmits.remove(conversationId);
        }

        // ---- Step 2 of attendance: the student answers the confirmation ----
        PendingConfirmation confirmation = getPendingConfirmation(conversationId);

        if (confirmation != null) {
            if (isRejection(question)) {
                pendingConfirmations.remove(conversationId);

                return saveAssistantReply(
                        conversation,
                        conversationId,
                        userMessage,
                        question,
                        "Okay, I won't mark attendance for "
                                + confirmation.subjectName() + ".",
                        intent);
            }

            if (isConfirmation(question, confirmation)) {
                pendingConfirmations.remove(conversationId);

                return saveAssistantReply(
                        conversation,
                        conversationId,
                        userMessage,
                        question,
                        recordConfirmedAttendance(confirmation),
                        intent);
            }

            // Not a yes/no: drop the confirmation and handle the message normally.
            pendingConfirmations.remove(conversationId);
        }

        PendingAttendance pending = getPendingAttendance(conversationId);

        if (pending != null && isCancelMessage(question)) {
            pendingAttendance.remove(conversationId);

            return saveAssistantReply(
                    conversation,
                    conversationId,
                    userMessage,
                    question,
                    "Okay, I've cancelled that attendance request.",
                    intent);
        }

        // ---- Step 1 of "add assignment" (supports multi-message follow-ups) ----
        PendingAddDraft addDraft = getPendingAddDraft(conversationId);

        if (addDraft != null && isCancelMessage(question)) {
            pendingAddDrafts.remove(conversationId);

            return saveAssistantReply(
                    conversation,
                    conversationId,
                    userMessage,
                    question,
                    "Okay, I've cancelled adding that assignment.",
                    intent);
        }

        boolean newAddRequest = isAddAssignmentRequest(question);

        // An attendance message while an add is pending drops the add.
        if (addDraft != null && !newAddRequest && isAttendanceRequest(question)) {
            pendingAddDrafts.remove(conversationId);
            addDraft = null;
        }

        if (addDraft != null || newAddRequest) {
            String combined = (addDraft == null || newAddRequest)
                    ? question
                    : addDraft.text() + "\n" + question;

            AddOutcome addOutcome =
                    handleAddAssignmentRequest(user, conversation, combined);

            if (addOutcome.confirmation() != null) {
                pendingAddDrafts.remove(conversationId);
                pendingAttendance.remove(conversationId);
                pendingSubmits.remove(conversationId);
                pendingConfirmations.remove(conversationId);
                pendingAdds.put(conversationId, addOutcome.confirmation());
            } else if (addOutcome.needsMoreInfo()) {
                pendingAttendance.remove(conversationId);
                pendingAddDrafts.put(
                        conversationId,
                        new PendingAddDraft(combined, Instant.now()));
            } else {
                pendingAddDrafts.remove(conversationId);
            }

            return saveAssistantReply(
                    conversation,
                    conversationId,
                    userMessage,
                    question,
                    addOutcome.reply(),
                    intent);
        }

        // ---- Step 1 of an assignment change ("make it submitted") ----
        if (pending == null && !isAttendanceRequest(question)) {
            SubmitOutcome submitOutcome =
                    handleSubmitRequest(conversationId, user, conversation, question);

            if (submitOutcome != null) {
                if (submitOutcome.pending() != null) {
                    pendingSubmits.put(conversationId, submitOutcome.pending());
                    pendingConfirmations.remove(conversationId);
                }

                return saveAssistantReply(
                        conversation,
                        conversationId,
                        userMessage,
                        question,
                        submitOutcome.reply(),
                        intent);
            }
        }

        // ---- Step 1 of attendance (supports multi-message follow-ups) ----
        if (pending != null || isAttendanceRequest(question)) {
            String combined = pending == null
                    ? question
                    : pending.text() + "\n" + question;

            AttendanceOutcome outcome =
                    handleAttendanceRequest(user, conversation, combined);

            if (outcome.confirmation() != null) {
                pendingAttendance.remove(conversationId);
                pendingSubmits.remove(conversationId);
                pendingConfirmations.put(conversationId, outcome.confirmation());
            } else if (outcome.needsMoreInfo()) {
                pendingAttendance.put(
                        conversationId,
                        new PendingAttendance(combined, Instant.now()));
            } else {
                pendingAttendance.remove(conversationId);
            }

            return saveAssistantReply(
                    conversation,
                    conversationId,
                    userMessage,
                    question,
                    outcome.reply(),
                    intent);
        }

        // ---- Document retrieval logic ----
        String documentContext = "";
        String subjectContext = "";
        String assignmentContext = "";

        Long semesterId = conversation.getSemesterId();

        if (semesterId != null && isOwnedSemester(semesterId, user)) {

            List<AcademicDocument> documents =
                    documentService.getDocumentsBySemester(semesterId);

            Set<Long> documentIds = new LinkedHashSet<>();

            for (AcademicDocument document : documents) {
                if ("PROCESSED".equalsIgnoreCase(
                        document.getProcessingStatus())) {
                    documentIds.add(document.getId());
                }
            }

            // Extract simple search terms from the question.
            Set<String> stopWords = Set.of(
                    "what", "when", "where", "which", "who",
                    "have", "has", "does", "did", "are", "was",
                    "were", "the", "and", "for", "from", "with",
                    "that", "this", "your", "my", "about",
                    "tell", "show", "mentioned", "listed",
                    "please", "semester", "subjects"
            );

            Set<String> keywords = new LinkedHashSet<>();

            for (String word : question.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9 ]", " ")
                    .split("\\s+")) {

                if (word.length() >= 3 && !stopWords.contains(word)) {
                    keywords.add(word);
                }

                if (keywords.size() >= 8) {
                    break;
                }
            }

            // Generic terms for broad timetable questions
            // (no hardcoded subject names).
            String lowerQuestion = question.toLowerCase(Locale.ROOT);

            if (lowerQuestion.contains("timetable")
                    || lowerQuestion.contains("schedule")
                    || lowerQuestion.contains("subjects")) {
                keywords.add("timetable");
                keywords.add("schedule");
                keywords.add("room");
                keywords.add("lecture");
                keywords.add("faculty");
            }

            StringBuilder context = new StringBuilder();

            // Store each chunk only once, even if multiple keywords match it.
            Map<Long, StudyMaterialChunk> uniqueChunks = new LinkedHashMap<>();

            for (Long documentId : documentIds) {
                for (String keyword : keywords) {

                    List<StudyMaterialChunk> matches =
                            chunkRepository.searchByDocumentAndKeyword(
                                    documentId, keyword);

                    for (StudyMaterialChunk chunk : matches) {
                        uniqueChunks.putIfAbsent(chunk.getId(), chunk);
                    }
                }
            }

            // Add unique chunks to the context, respecting the size limit.
            for (StudyMaterialChunk chunk : uniqueChunks.values()) {
                if (context.length() >= MAX_DOCUMENT_CONTEXT_CHARS) {
                    break;
                }

                String content = chunk.getContent();

                if (content == null || content.isBlank()) {
                    continue;
                }

                String entry = "\n\nRelevant study material:\n" + content;

                int remaining = MAX_DOCUMENT_CONTEXT_CHARS - context.length();

                context.append(
                        entry.substring(0, Math.min(entry.length(), remaining))
                );
            }

            documentContext = context.toString();

            // Verified data for the exact subject(s) named in the question,
            // so "X" and "X Lab" are never mixed together.
            List<Subject> allSubjects =
                    subjectRepository.findBySemesterId(semesterId);

            subjectContext = buildSubjectContext(
                    allSubjects,
                    findMentionedSubjects(allSubjects, question));

            // Verified assignment and submission records from the database.
            assignmentContext = buildAssignmentContext(
                    conversationId, allSubjects, question);
        }

        String prompt;

        if (documentContext.isBlank() && subjectContext.isBlank()
                && assignmentContext.isBlank()) {
            prompt = """
                    You are SmartSem, an AI academic assistant for B.Tech students.
                    Answer the student's question clearly and simply, with examples
                    where useful.

                    Rules:
                    - For general academic questions, answer using your general knowledge.
                    - For questions about the student's own timetable, subjects, rooms,
                      faculty, attendance, or assignments, do not invent information.
                    - If the required college-specific data is unavailable, clearly say:
                      "I don't have this information for your current semester yet.
                      Please add your subjects, timetable, or relevant documents first."
                    - Never make up class timings, rooms, faculty names, or records.
                    - Answer only what was asked.

                    Student's question:
                    %s
                    """.formatted(question);
        } else {
            prompt = """
                    You are SmartSem, an AI academic assistant for B.Tech students.

                    Below is context from the student's uploaded academic documents
                    (timetable, notes, etc.). Use it ONLY when the question is about
                    the student's own college information.

                    Rules:
                    - For questions about the student's own timetable, subjects,
                      rooms, faculty, assignments, or records, answer ONLY from the
                      data below. If it doesn't have the answer, say the uploaded
                      material doesn't contain it. Never invent college-specific
                      details.
                    - For questions about assignments, deadlines or submissions, the
                      "Verified assignment data" is the source of truth. A listed
                      submission with a timestamp means the assignment was submitted
                      at that time. If the status and the submissions disagree, state
                      both facts plainly.
                    - Never claim that you changed or added an assignment or
                      submission record yourself. If the student asks for that, tell
                      them to say it directly, for example "mark <assignment name> as
                      submitted" or "add an assignment <title> for <subject> due
                      <date>", and that you will ask them to confirm first.
                    - A theory subject and its lab (for example "X" and "X Lab")
                      are SEPARATE subjects. Answer only about the one the student
                      named. Mention the other only if the student asks about both
                      or about the lab.
                    - For general academic or conceptual questions (for example
                      "what is cloud computing"), answer from your general knowledge
                      only. Do NOT mention the student's documents, subjects, rooms,
                      faculty, or timetable, and do not add any closing remark about
                      their curriculum, unless the student explicitly asks about them.
                    - Answer only what was asked. Do not add extra commentary.
                    - The extracted text may contain OCR errors. Interpret obvious
                      errors carefully.

                    Verified subject and timetable data (most reliable):
                    %s

                    Verified assignment data (most reliable for assignments):
                    %s

                    Academic document context (may mix subjects, use carefully):
                    %s

                    Student's question:
                    %s
                    """.formatted(
                    subjectContext.isBlank() ? "None matched." : subjectContext,
                    assignmentContext.isBlank()
                            ? "Not relevant to this question."
                            : assignmentContext,
                    documentContext.isBlank() ? "None retrieved." : documentContext,
                    question);
        }

        // Tell Gemini what this chat can and cannot do (applies to both prompts).
        prompt = CAPABILITY_RULES + "\n" + prompt;

        String answer;

        try {
            answer = geminiService.askGemini(prompt);
        } catch (Exception e) {
            if (isQuotaError(e)) {
                return saveAssistantReply(
                        conversation,
                        conversationId,
                        userMessage,
                        question,
                        QUOTA_MESSAGE,
                        intent);
            }

            System.err.println("Gemini error type: "
                    + e.getClass().getName());
            System.err.println("Gemini error message: "
                    + e.getMessage());

            if (e.getCause() != null) {
                System.err.println("Root cause: "
                        + e.getCause().getClass().getName()
                        + " - " + e.getCause().getMessage());
            }

            e.printStackTrace();

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI service failed. Check server logs.",
                    e
            );
        }

        return saveAssistantReply(
                conversation,
                conversationId,
                userMessage,
                question,
                answer,
                intent);
    }

    // ---------------- Helper methods (outside chat()) ----------------

    private boolean isOwnedSemester(Long semesterId, User user) {
        return semesterRepository.findById(semesterId)
                .filter(s -> s.getUserId().equals(user.getId()))
                .isPresent();
    }

    private boolean isAttendanceRequest(String message) {
        String text = message.toLowerCase(Locale.ROOT);

        // Questions are not attendance records.
        if (text.trim().endsWith("?")) {
            return false;
        }

        for (Pattern pattern : ATTENDANCE_PATTERNS) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    private boolean isAddAssignmentRequest(String message) {
        String text = message.toLowerCase(Locale.ROOT)
                .replace("'", "").replace("’", "").trim();

        // Questions ("how do I add an assignment?") are not requests.
        if (text.endsWith("?")) {
            return false;
        }

        // "mark this assignment as submitted" is a submit request, not an add.
        if (SUBMIT_PATTERNS.stream().anyMatch(p -> p.matcher(text).find())) {
            return false;
        }

        return ADD_ASSIGNMENT_PATTERNS.stream()
                .anyMatch(p -> p.matcher(text).find());
    }

    private boolean isCancelMessage(String message) {
        return CANCEL_WORDS.contains(
                message.toLowerCase(Locale.ROOT).trim());
    }

    private boolean isQuotaError(Throwable t) {
        while (t != null) {
            String m = t.getMessage();

            if (m != null && (m.contains("429")
                    || m.contains("RESOURCE_EXHAUSTED")
                    || m.toLowerCase(Locale.ROOT).contains("quota"))) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    /** Returns the pending attendance request, or null if none/expired. */
    private PendingAttendance getPendingAttendance(Long conversationId) {
        PendingAttendance pending = pendingAttendance.get(conversationId);

        if (pending == null) {
            return null;
        }

        if (Duration.between(pending.createdAt(), Instant.now())
                .compareTo(PENDING_TTL) > 0) {
            pendingAttendance.remove(conversationId);
            return null;
        }

        return pending;
    }

    /** Returns the pending confirmation, or null if none/expired. */
    private PendingConfirmation getPendingConfirmation(Long conversationId) {
        PendingConfirmation pending = pendingConfirmations.get(conversationId);

        if (pending == null) {
            return null;
        }

        if (Duration.between(pending.createdAt(), Instant.now())
                .compareTo(PENDING_TTL) > 0) {
            pendingConfirmations.remove(conversationId);
            return null;
        }

        return pending;
    }

    /** Returns the pending assignment change, or null if none/expired. */
    private PendingSubmit getPendingSubmit(Long conversationId) {
        PendingSubmit pending = pendingSubmits.get(conversationId);

        if (pending == null) {
            return null;
        }

        if (Duration.between(pending.createdAt(), Instant.now())
                .compareTo(PENDING_TTL) > 0) {
            pendingSubmits.remove(conversationId);
            return null;
        }

        return pending;
    }

    /** Returns the pending "add assignment" draft, or null if none/expired. */
    private PendingAddDraft getPendingAddDraft(Long conversationId) {
        PendingAddDraft pending = pendingAddDrafts.get(conversationId);

        if (pending == null) {
            return null;
        }

        if (Duration.between(pending.createdAt(), Instant.now())
                .compareTo(PENDING_TTL) > 0) {
            pendingAddDrafts.remove(conversationId);
            return null;
        }

        return pending;
    }

    /** Returns the pending new assignment awaiting confirmation, or null if none/expired. */
    private PendingAdd getPendingAdd(Long conversationId) {
        PendingAdd pending = pendingAdds.get(conversationId);

        if (pending == null) {
            return null;
        }

        if (Duration.between(pending.createdAt(), Instant.now())
                .compareTo(PENDING_TTL) > 0) {
            pendingAdds.remove(conversationId);
            return null;
        }

        return pending;
    }

    private static String normalizeReply(String message) {
        return message.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isRejection(String message) {
        return NO_WORDS.contains(normalizeReply(message));
    }

    private boolean isYes(String message) {
        return YES_WORDS.contains(normalizeReply(message));
    }

    private boolean isConfirmation(String message, PendingConfirmation pending) {
        String t = normalizeReply(message);

        if (YES_WORDS.contains(t)) {
            return true;
        }

        if ("PRESENT".equals(pending.status())) {
            return PRESENT_CONFIRM.matcher(t).matches();
        }

        return ABSENT_CONFIRM.matcher(t).matches();
    }

    /** Saves the attendance the student just confirmed. */
    private String recordConfirmedAttendance(PendingConfirmation c) {
        try {
            attendanceRecordService.recordAttendanceForClass(
                    c.subjectId(),
                    c.timetableEntryId(),
                    c.classDate(),
                    c.status(),
                    "Recorded through SmartSem chat");

            return "Done! I marked you " + c.status() + " for "
                    + c.subjectName() + " on " + c.classDate()
                    + " (" + c.timeRange() + ").";

        } catch (IllegalStateException e) {
            return "Attendance already appears to be recorded for "
                    + c.subjectName() + " on " + c.classDate() + ".";

        } catch (IllegalArgumentException e) {
            return "I couldn't save that attendance record. Please try again.";
        }
    }

    private AttendanceOutcome done(String reply) {
        return new AttendanceOutcome(reply, false, null);
    }

    private AttendanceOutcome ask(String reply) {
        return new AttendanceOutcome(reply, true, null);
    }

    private AttendanceOutcome confirm(String reply, PendingConfirmation c) {
        return new AttendanceOutcome(reply, false, c);
    }

    private AddOutcome addDone(String reply) {
        return new AddOutcome(reply, false, null);
    }

    private AddOutcome addAsk(String reply) {
        return new AddOutcome(reply, true, null);
    }

    private AddOutcome addConfirm(String reply, PendingAdd p) {
        return new AddOutcome(reply, false, p);
    }

    // ---------- Subject matching (keeps "X" and "X Lab" separate) ----------

    /** Lower-cases and strips punctuation, padded with spaces for whole-word matching. */
    private static String norm(String s) {
        return " " + s.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim() + " ";
    }

    /** Subjects whose full name appears in the question; the longest name wins. */
    private List<Subject> findMentionedSubjects(
            List<Subject> subjects, String question) {

        String q = norm(question);

        List<Subject> matched = new ArrayList<>();

        for (Subject s : subjects) {
            if (s.getName() != null && q.contains(norm(s.getName()))) {
                matched.add(s);
            }
        }

        // If "X Lab" is mentioned, drop "X" (it is only a substring match).
        return matched.stream()
                .filter(s -> matched.stream().noneMatch(o ->
                        norm(o.getName()).length() > norm(s.getName()).length()
                                && norm(o.getName()).contains(norm(s.getName()))))
                .toList();
    }

    /** Other subjects whose names contain, or are contained in, the focus subject's name. */
    private List<Subject> findLookalikes(List<Subject> all, Subject focus) {
        String f = norm(focus.getName());

        return all.stream()
                .filter(o -> !o.getId().equals(focus.getId()))
                .filter(o -> o.getName() != null)
                .filter(o -> {
                    String n = norm(o.getName());
                    return n.contains(f) || f.contains(n);
                })
                .toList();
    }

    /** Builds verified per-subject timetable text for the subjects named in the question. */
    private String buildSubjectContext(
            List<Subject> allSubjects, List<Subject> focused) {

        if (focused.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (Subject s : focused) {
            sb.append("Subject the student asked about: ")
                    .append(s.getName()).append("\n");

            for (TimetableEntry e :
                    timetableEntryRepository.findBySubjectId(s.getId())) {

                if (e.getDayOfWeek() == null) {
                    continue;
                }

                int day = e.getDayOfWeek().intValue();

                if (day < 1 || day > 7) {
                    continue;
                }

                sb.append("  - ")
                        .append(DayOfWeek.of(day)
                                .getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                        .append(" ").append(e.getStartTime())
                        .append("-").append(e.getEndTime())
                        .append("\n");
            }

            for (Subject other : findLookalikes(allSubjects, s)) {
                sb.append("A DIFFERENT subject with a similar name "
                                + "(do NOT mix it in): ")
                        .append(other.getName()).append("\n");
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    // ---------- Assignments ----------

    private static String fmt(OffsetDateTime t) {
        return t == null ? "not set" : DISPLAY_TIME.format(t) + " IST";
    }

    /** All assignments of the given subjects, with their subject names. */
    private List<AssignmentRef> listAssignments(List<Subject> subjects) {
        List<AssignmentRef> refs = new ArrayList<>();

        for (Subject s : subjects) {
            for (Assignment a : assignmentRepository.findBySubjectId(s.getId())) {
                refs.add(new AssignmentRef(a, s.getName()));
            }
        }

        return refs;
    }

    /** Assignments whose full title appears in the given text. */
    private List<AssignmentRef> titleMatches(List<AssignmentRef> refs, String text) {
        String q = norm(text);

        return refs.stream()
                .filter(r -> r.assignment().getTitle() != null
                        && q.contains(norm(r.assignment().getTitle())))
                .toList();
    }

    /**
     * Verified assignment and submission data for assignment-related questions.
     * If the question names an assignment, only that one is returned;
     * otherwise all assignments of the semester (up to 25) are returned.
     * Also remembers the assignment being discussed, for "make it submitted".
     */
    private String buildAssignmentContext(
            Long conversationId, List<Subject> subjects, String question) {

        if (!ASSIGNMENT_WORDS.matcher(question.toLowerCase(Locale.ROOT)).find()) {
            return "";
        }

        List<AssignmentRef> refs = listAssignments(subjects);
        List<AssignmentRef> matched = titleMatches(refs, question);

        if (matched.size() == 1) {
            lastAssignmentByConversation.put(
                    conversationId, matched.get(0).assignment().getId());
        }

        List<AssignmentRef> chosen = matched.isEmpty() ? refs : matched;

        if (chosen.isEmpty()) {
            return "No assignments are recorded for this semester.";
        }

        List<String> lines = new ArrayList<>();

        for (AssignmentRef ref : chosen) {
            Assignment a = ref.assignment();

            StringBuilder sb = new StringBuilder("- ")
                    .append(a.getTitle())
                    .append(" (").append(ref.subjectName()).append(")")
                    .append(", status: ").append(a.getStatus())
                    .append(" (last updated ").append(fmt(a.getUpdatedAt())).append(")")
                    .append(", deadline: ").append(fmt(a.getDeadline()));

            List<AssignmentSubmission> subs =
                    assignmentSubmissionRepository.findByAssignmentId(a.getId());

            if (subs.isEmpty()) {
                sb.append(", no file submission recorded");
            } else {
                for (AssignmentSubmission sub : subs) {
                    sb.append("; submission: file \"")
                            .append(sub.getFileName())
                            .append("\" submitted at ")
                            .append(fmt(sub.getSubmittedAt()));
                }
            }

            lines.add(sb.toString());
        }

        return lines.stream().limit(25).collect(Collectors.joining("\n"));
    }

    private String askWhich(List<AssignmentRef> options) {
        String list = options.stream()
                .limit(10)
                .map(r -> "- " + r.assignment().getTitle()
                        + " (" + r.subjectName() + "), status: "
                        + r.assignment().getStatus())
                .collect(Collectors.joining("\n"));

        return "Which assignment do you mean?\n\n" + list
                + "\n\nPlease say it with the name, for example: "
                + "\"mark " + options.get(0).assignment().getTitle()
                + " as submitted\".";
    }

    /**
     * Step 1 of marking an assignment as submitted. Returns null when the
     * message is not a submit request, so normal chat handles it. It never
     * saves anything: it returns a confirmation question.
     */
    private SubmitOutcome handleSubmitRequest(
            Long conversationId,
            User user,
            ChatConversation conversation,
            String message) {

        String text = message.toLowerCase(Locale.ROOT)
                .replace("'", "").replace("’", "").trim();

        // Questions are not requests to change anything.
        if (text.endsWith("?")) {
            return null;
        }

        boolean looksLikeSubmit = SUBMIT_PATTERNS.stream()
                .anyMatch(p -> p.matcher(text).find());

        if (!looksLikeSubmit) {
            return null;
        }

        Long semesterId = conversation.getSemesterId();

        if (semesterId == null || !isOwnedSemester(semesterId, user)) {
            return null;
        }

        List<AssignmentRef> refs = listAssignments(
                subjectRepository.findBySemesterId(semesterId));

        boolean mentionsAssignment = ASSIGNMENT_NOUNS.matcher(text).find();
        boolean refersToIt = REFERS_TO_IT.matcher(text).find();

        List<AssignmentRef> byTitle = titleMatches(refs, text);

        AssignmentRef target = null;

        if (byTitle.size() == 1) {
            target = byTitle.get(0);

        } else if (byTitle.size() > 1) {
            return new SubmitOutcome(askWhich(byTitle), null);

        } else if (refersToIt) {
            Long lastId = lastAssignmentByConversation.get(conversationId);

            if (lastId != null) {
                target = refs.stream()
                        .filter(r -> r.assignment().getId().equals(lastId))
                        .findFirst()
                        .orElse(null);
            }
        }

        if (target == null) {
            // Not clearly about assignments: leave it to normal chat.
            if (!mentionsAssignment && !refersToIt) {
                return null;
            }

            List<AssignmentRef> open = refs.stream()
                    .filter(r -> !SUBMITTED_STATUS.equalsIgnoreCase(
                            r.assignment().getStatus()))
                    .toList();

            if (open.isEmpty()) {
                return new SubmitOutcome(
                        "You have no pending assignments in this semester.", null);
            }

            if (open.size() > 1) {
                return new SubmitOutcome(askWhich(open), null);
            }

            target = open.get(0);
        }

        Assignment a = target.assignment();

        if (SUBMITTED_STATUS.equalsIgnoreCase(a.getStatus())) {
            return new SubmitOutcome(
                    "**" + a.getTitle() + "** is already marked as "
                            + a.getStatus() + ".", null);
        }

        PendingSubmit pendingSubmit = new PendingSubmit(
                a.getId(), a.getTitle(), target.subjectName(), Instant.now());

        String reply = "Just to confirm: mark **" + a.getTitle() + "** ("
                + target.subjectName() + ") as " + SUBMITTED_STATUS
                + "? It is currently " + a.getStatus() + ", with deadline "
                + fmt(a.getDeadline()) + ".\n\n"
                + "Reply **yes** to confirm, or **no** to cancel.";

        return new SubmitOutcome(reply, pendingSubmit);
    }

    /** Saves the status change the student just confirmed. */
    private String recordConfirmedSubmission(PendingSubmit s) {
        Assignment a = assignmentRepository.findById(s.assignmentId()).orElse(null);

        if (a == null) {
            return "I couldn't find that assignment any more. Please try again.";
        }

        if (SUBMITTED_STATUS.equalsIgnoreCase(a.getStatus())) {
            return "**" + a.getTitle() + "** is already marked as "
                    + a.getStatus() + ".";
        }

        a.setStatus(SUBMITTED_STATUS);
        assignmentRepository.save(a);

        return "Done! I marked **" + a.getTitle() + "** as " + SUBMITTED_STATUS
                + " on " + fmt(OffsetDateTime.now()) + ".";
    }

    // ---------- Adding a new assignment from chat ----------

    /** Parses a deadline given as an offset date-time, a local date-time or a date. */
    private OffsetDateTime parseDeadline(String text) {
        try {
            return OffsetDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
            // try the next format
        }

        try {
            return LocalDateTime.parse(text).atZone(IST).toOffsetDateTime();
        } catch (DateTimeParseException ignored) {
            // try the next format
        }

        try {
            // Only a date was given: use the end of that day.
            return LocalDate.parse(text).atTime(23, 59).atZone(IST).toOffsetDateTime();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static boolean isBlankValue(String value) {
        return value == null || value.isBlank() || "null".equalsIgnoreCase(value.trim());
    }

    /**
     * Step 1 of adding an assignment. Gemini extracts the title, subject and
     * deadline, and everything is validated against the database. Nothing is
     * saved here: on success it returns a confirmation question.
     */
    private AddOutcome handleAddAssignmentRequest(
            User user,
            ChatConversation conversation,
            String message) {

        Long semesterId = conversation.getSemesterId();

        if (semesterId == null) {
            return addDone("Please select your semester before adding an assignment.");
        }

        if (!isOwnedSemester(semesterId, user)) {
            return addDone("I couldn't verify your semester. Please try again.");
        }

        List<Subject> allSubjects = subjectRepository.findBySemesterId(semesterId);

        if (allSubjects.isEmpty()) {
            return addDone(
                    "There are no subjects in this semester yet. "
                            + "Please add your subjects first, then try again.");
        }

        LocalDate today = LocalDate.now(IST);

        String subjectList = allSubjects.stream()
                .map(s -> "- " + s.getName())
                .collect(Collectors.joining("\n"));

        String prompt = """
                You are SmartSem, a college assistant.
                Interpret this request to add a new assignment. The text below
                may contain several student messages (oldest first); combine
                them into one request.

                The student's subjects are:
                %s

                Rules:
                - subjectName must be the EXACT name from the list above.
                - A theory subject and its lab (for example "X" and "X Lab")
                  are DIFFERENT subjects. If the student does not say "lab",
                  choose the non-lab subject. If they say "lab", choose the lab.
                - title is the assignment's name exactly as the student gave it.
                  Never invent a title.
                - deadline: use "YYYY-MM-DDTHH:mm" (24-hour, India time) when a
                  time is given, or "YYYY-MM-DD" when only a date is given.
                - If the student says there is no deadline, set noDeadline to
                  true and deadline to null.
                - Resolve words like "today", "tomorrow" and "next Friday"
                  using today's date.
                - If the title, the subject or the deadline is missing or
                  ambiguous, use null for it, set needsClarification to true
                  and ask one short question in clarificationQuestion.
                - Never invent the title, subject or deadline.

                Return ONLY valid JSON:
                {
                  "action": "ADD_ASSIGNMENT",
                  "title": "assignment title or null",
                  "subjectName": "exact subject name or null",
                  "deadline": "YYYY-MM-DDTHH:mm or YYYY-MM-DD or null",
                  "noDeadline": false,
                  "needsClarification": false,
                  "clarificationQuestion": ""
                }

                Today's date is %s (%s).

                Student messages:
                %s
                """.formatted(
                subjectList,
                today,
                today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                message);

        final JsonNode result;

        try {
            result = parseActionResponse(
                    geminiService.askGemini(prompt), "ADD_ASSIGNMENT");
        } catch (Exception e) {
            if (isQuotaError(e)) {
                return addDone(QUOTA_MESSAGE);
            }
            return addDone("I couldn't understand that. Please tell me the assignment title, the subject and the deadline.");
        }

        String title = result.path("title").asText("").trim();
        String subjectName = result.path("subjectName").asText("").trim();
        String deadlineText = result.path("deadline").asText("").trim();
        boolean noDeadline = result.path("noDeadline").asBoolean(false);
        boolean needsClarification =
                result.path("needsClarification").asBoolean(true);

        if (needsClarification
                || isBlankValue(title)
                || isBlankValue(subjectName)
                || (!noDeadline && isBlankValue(deadlineText))) {

            String clarificationQuestion =
                    result.path("clarificationQuestion").asText("");

            return addAsk(clarificationQuestion.isBlank()
                    ? "Please tell me the assignment title, the subject, and the deadline (or say \"no deadline\")."
                    : clarificationQuestion);
        }

        if (title.length() > MAX_TITLE_LENGTH) {
            return addAsk("That title is too long. Please give a shorter title (up to "
                    + MAX_TITLE_LENGTH + " characters).");
        }

        Subject subject = subjectRepository
                .findBySemesterIdAndNameIgnoreCase(semesterId, subjectName)
                .orElse(null);

        if (subject == null) {
            return addAsk("I couldn't find '" + subjectName
                    + "' in your selected semester. Please give the exact subject name.");
        }

        OffsetDateTime deadline = null;

        if (!noDeadline) {
            deadline = parseDeadline(deadlineText);

            if (deadline == null) {
                return addAsk("I couldn't read that deadline. Please give a date, for example 2026-10-30 5 pm.");
            }

            if (deadline.isBefore(OffsetDateTime.now())) {
                return addAsk("That deadline (" + fmt(deadline)
                        + ") is in the past. Please give a future deadline.");
            }
        }

        boolean duplicate = assignmentRepository.findBySubjectId(subject.getId())
                .stream()
                .anyMatch(a -> a.getTitle() != null
                        && norm(a.getTitle()).equals(norm(title)));

        if (duplicate) {
            return addDone("**" + title + "** already exists for "
                    + subject.getName() + ". I haven't added it again.");
        }

        PendingAdd pendingAdd = new PendingAdd(
                subject.getId(), subject.getName(), title, deadline, Instant.now());

        String reply = "Just to confirm: add a new assignment **" + title
                + "** for **" + subject.getName() + "**, "
                + (deadline == null ? "with no deadline" : "due " + fmt(deadline))
                + "?\n\nReply **yes** to add it, or **no** to cancel.";

        return addConfirm(reply, pendingAdd);
    }

    /** Saves the new assignment the student just confirmed. */
    private String recordConfirmedAddition(Long conversationId, PendingAdd p) {
        boolean duplicate = assignmentRepository.findBySubjectId(p.subjectId())
                .stream()
                .anyMatch(a -> a.getTitle() != null
                        && norm(a.getTitle()).equals(norm(p.title())));

        if (duplicate) {
            return "**" + p.title() + "** already exists for "
                    + p.subjectName() + ". I haven't added it again.";
        }

        Assignment a = new Assignment();
        a.setSubjectId(p.subjectId());
        a.setTitle(p.title());
        a.setDeadline(p.deadline());
        a.setStatus(PENDING_STATUS);

        Assignment saved = assignmentRepository.save(a);

        // So a follow-up like "make it submitted" knows which assignment "it" is.
        lastAssignmentByConversation.put(conversationId, saved.getId());

        return "Done! I added **" + saved.getTitle() + "** for "
                + p.subjectName() + ", "
                + (saved.getDeadline() == null
                        ? "with no deadline"
                        : "due " + fmt(saved.getDeadline()))
                + ". Its status is " + PENDING_STATUS + ".";
    }

    /**
     * Interprets an attendance conversation with Gemini and validates it
     * against the user's subjects and timetable. It does NOT save anything:
     * on success it returns a confirmation question for the student.
     */
    private AttendanceOutcome handleAttendanceRequest(
            User user,
            ChatConversation conversation,
            String message) {

        Long semesterId = conversation.getSemesterId();

        if (semesterId == null) {
            return done("Please select your semester before recording attendance.");
        }

        if (!isOwnedSemester(semesterId, user)) {
            return done("I couldn't verify your semester. Please try again.");
        }

        LocalDate today = LocalDate.now(IST);

        List<Subject> allSubjects = subjectRepository.findBySemesterId(semesterId);

        if (allSubjects.isEmpty()) {
            return done(
                    "There are no subjects in this semester yet. "
                            + "Please add your subjects and timetable first, "
                            + "then try again.");
        }

        String subjectList = allSubjects.stream()
                .map(s -> "- " + s.getName())
                .collect(Collectors.joining("\n"));

        String prompt = """
                You are SmartSem, a college assistant.
                Interpret this attendance request. The text below may contain
                several student messages (oldest first); combine them into one
                request.

                The student's subjects are:
                %s

                Rules for subjectName:
                - Use the EXACT name from the list above.
                - A theory subject and its lab (for example "X" and "X Lab")
                  are DIFFERENT subjects. If the student does not say "lab",
                  choose the non-lab subject. If they say "lab", choose the lab.
                - If nothing matches or it is ambiguous, use null and ask.

                Return ONLY valid JSON:
                {
                  "action": "MARK_ATTENDANCE",
                  "subjectName": "exact subject name or null",
                  "date": "YYYY-MM-DD or null",
                  "status": "PRESENT, ABSENT, or null",
                  "startTime": "HH:mm (24-hour) or null",
                  "needsClarification": false,
                  "clarificationQuestion": ""
                }

                Set needsClarification to true if any required
                information is missing or ambiguous.
                Never invent the subject, date, status, or class time.
                Resolve words like "today" and "yesterday" using today's date.
                Today's date is %s.

                Student messages:
                %s
                """.formatted(subjectList, today, message);

        final JsonNode result;

        try {
            result = parseActionResponse(
                    geminiService.askGemini(prompt), "MARK_ATTENDANCE");
        } catch (Exception e) {
            if (isQuotaError(e)) {
                return done(QUOTA_MESSAGE);
            }
            return done("I couldn't interpret that request. Please specify the subject, date, and attendance status.");
        }

        String clarification = getAttendanceClarification(result);

        if (clarification != null) {
            return ask(clarification);
        }

        String subjectName = result.path("subjectName").asText("").trim();
        String dateText = result.path("date").asText("").trim();
        String status = result.path("status").asText("")
                .trim().toUpperCase(Locale.ROOT);

        final LocalDate classDate;

        try {
            classDate = LocalDate.parse(dateText);
        } catch (DateTimeParseException e) {
            return ask("Please provide a valid class date, for example 2026-10-23 or 'yesterday'.");
        }

        if (classDate.isAfter(today)) {
            return ask("That date is in the future. Please give a class date that has already happened.");
        }

        Subject subject = subjectRepository
                .findBySemesterIdAndNameIgnoreCase(semesterId, subjectName)
                .orElse(null);

        if (subject == null) {
            return ask("I couldn't find '" + subjectName
                    + "' in your selected semester. Please give the exact subject name.");
        }

        short dayOfWeek = (short) classDate.getDayOfWeek().getValue();
        String dayName = classDate.getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        List<TimetableEntry> sameDay =
                timetableEntryRepository.findBySubjectId(subject.getId())
                        .stream()
                        .filter(slot ->
                                slot.getDayOfWeek() != null
                                && slot.getDayOfWeek().shortValue() == dayOfWeek
                                && slot.getStartTime() != null)
                        .toList();

        if (sameDay.isEmpty()) {
            return ask("I couldn't find a " + subject.getName()
                    + " class on " + dayName + " " + classDate
                    + " in your timetable. Please check the date.");
        }

        String startTimeText = result.path("startTime").asText("");
        List<TimetableEntry> matchingEntries;

        if (startTimeText.isBlank()
                || "null".equalsIgnoreCase(startTimeText)) {

            if (sameDay.size() > 1) {
                String times = sameDay.stream()
                        .map(slot -> slot.getStartTime().toString())
                        .collect(Collectors.joining(", "));

                return ask("You have " + sameDay.size() + " "
                        + subject.getName() + " classes on " + dayName
                        + " (" + times + "). Which start time did you attend? Please use HH:mm.");
            }

            matchingEntries = sameDay;

        } else {
            LocalTime parsed;

            try {
                parsed = LocalTime.parse(startTimeText);
            } catch (DateTimeParseException e) {
                return ask("Please provide the class start time as HH:mm, for example 09:00.");
            }

            final LocalTime startTime = parsed;

            matchingEntries = sameDay.stream()
                    .filter(slot -> slot.getStartTime().equals(startTime))
                    .toList();

            if (matchingEntries.isEmpty()) {
                return ask("I couldn't find a " + subject.getName()
                        + " class starting at " + startTime
                        + " on " + dayName + " " + classDate
                        + ". Please check the time.");
            }
        }

        if (matchingEntries.size() > 1) {
            return ask("Multiple timetable entries match this class. Please clarify which class or group you attended.");
        }

        TimetableEntry timetableEntry = matchingEntries.get(0);

        String timeRange = timetableEntry.getStartTime()
                + "–" + timetableEntry.getEndTime();

        PendingConfirmation pendingConfirmation = new PendingConfirmation(
                subject.getId(),
                subject.getName(),
                timetableEntry.getId(),
                classDate,
                timeRange,
                status,
                Instant.now());

        String confirmationText = "PRESENT".equals(status)
                ? "Just to confirm: did you attend **" + subject.getName()
                + "** on " + dayName + ", " + classDate
                + " (" + timeRange + ")?\n\n"
                + "Reply **yes** (or \"I attended\") to mark you PRESENT, "
                + "or **no** to cancel."
                : "Just to confirm: you were absent for **" + subject.getName()
                + "** on " + dayName + ", " + classDate
                + " (" + timeRange + ")?\n\n"
                + "Reply **yes** to mark you ABSENT, or **no** to cancel.";

        return confirm(confirmationText, pendingConfirmation);
    }

    /** Saves the assistant message, bumps the conversation, builds the response. */
    private ChatResponse saveAssistantReply(
            ChatConversation conversation,
            Long conversationId,
            ChatMessage userMessage,
            String question,
            String answer,
            String intent) {

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setConversationId(conversationId);
        assistantMessage.setRole("ASSISTANT");
        assistantMessage.setMessageText(answer);
        assistantMessage.setIntent(intent);

        assistantMessage = chatMessageService.saveMessage(assistantMessage);

        conversation.setUpdatedAt(OffsetDateTime.now());
        conversationRepository.save(conversation);

        return new ChatResponse(
                conversationId,
                userMessage.getId(),
                question,
                assistantMessage.getId(),
                answer,
                intent
        );
    }

    /** Parses Gemini's JSON reply and checks that it has the expected "action". */
    private JsonNode parseActionResponse(String response, String expectedAction) {
        try {
            String json = response.trim();

            // Handle responses wrapped in Markdown code fences.
            if (json.startsWith("```")) {
                json = json.replaceFirst("^```(?:json)?\\s*", "");
                json = json.replaceFirst("\\s*```$", "");
            }

            JsonNode result = objectMapper.readTree(json);

            if (result == null || !result.isObject()) {
                throw new IllegalArgumentException(
                        "Response must be a JSON object.");
            }

            if (!expectedAction.equals(
                    result.path("action").asText())) {
                throw new IllegalArgumentException(
                        "Unexpected action.");
            }

            return result;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not interpret the request. Please rephrase it.",
                    e);
        }
    }

    private String getAttendanceClarification(JsonNode result) {
        String subjectName = result.path("subjectName").asText("");
        String date = result.path("date").asText("");
        String status = result.path("status").asText("");
        boolean needsClarification =
                result.path("needsClarification").asBoolean(true);

        if (needsClarification
                || subjectName.isBlank()
                || date.isBlank()
                || status.isBlank()
                || "null".equalsIgnoreCase(subjectName)
                || "null".equalsIgnoreCase(date)
                || "null".equalsIgnoreCase(status)) {

            String clarificationQuestion =
                    result.path("clarificationQuestion").asText("");

            return clarificationQuestion.isBlank()
                    ? "Please specify the subject, class date, and whether you were present or absent."
                    : clarificationQuestion;
        }

        if (!"PRESENT".equalsIgnoreCase(status)
                && !"ABSENT".equalsIgnoreCase(status)) {
            return "Please confirm whether you attended the class or were absent.";
        }

        return null;
    }
}