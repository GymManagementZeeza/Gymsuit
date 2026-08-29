package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.exception.ConflictException;
import com.zeezaglobal.gymmanagement.repository.TrainingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** A trainer can only train one person at a time — shared by direct session booking and session-request approval. */
@Component
@RequiredArgsConstructor
public class SessionAvailabilityChecker {

    /** Sessions without an explicit end time are assumed to hold the slot for this long. */
    private static final long DEFAULT_DURATION_MINUTES = 60;

    private final TrainingSessionRepository sessionRepository;

    /**
     * Throws if the trainer already has an overlapping session. Pass excludeSessionId when checking
     * a reschedule of an existing session, so it doesn't conflict with itself.
     */
    public void ensureTrainerFree(Long trainerId, LocalDateTime startTime, LocalDateTime endTime, Long excludeSessionId) {
        LocalDateTime effectiveEnd = effectiveEnd(startTime, endTime);
        boolean conflict = sessionRepository.findAllByTrainerId(trainerId).stream()
                .filter(session -> excludeSessionId == null || !session.getId().equals(excludeSessionId))
                .anyMatch(session -> overlaps(
                        session.getStartTime(), effectiveEnd(session.getStartTime(), session.getEndTime()),
                        startTime, effectiveEnd));
        if (conflict) {
            throw new ConflictException("This trainer already has a session booked at that time");
        }
    }

    private LocalDateTime effectiveEnd(LocalDateTime start, LocalDateTime end) {
        return end != null ? end : start.plusMinutes(DEFAULT_DURATION_MINUTES);
    }

    private boolean overlaps(LocalDateTime aStart, LocalDateTime aEnd, LocalDateTime bStart, LocalDateTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }
}
