package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.SessionEnrollmentResponse;
import com.zeezaglobal.gymmanagement.dto.SessionRequest;
import com.zeezaglobal.gymmanagement.dto.SessionResponse;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.entity.SessionEnrollment;
import com.zeezaglobal.gymmanagement.entity.Trainer;
import com.zeezaglobal.gymmanagement.entity.TrainingSession;
import com.zeezaglobal.gymmanagement.exception.ConflictException;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.MemberRepository;
import com.zeezaglobal.gymmanagement.repository.SessionEnrollmentRepository;
import com.zeezaglobal.gymmanagement.repository.TrainerRepository;
import com.zeezaglobal.gymmanagement.repository.TrainingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final TrainingSessionRepository sessionRepository;
    private final SessionEnrollmentRepository enrollmentRepository;
    private final TrainerRepository trainerRepository;
    private final MemberRepository memberRepository;
    private final GymService gymService;
    private final SessionAvailabilityChecker availabilityChecker;

    public List<SessionResponse> findAllByGym(Long gymId) {
        gymService.getGymOrThrow(gymId);
        return sessionRepository.findAllByGymId(gymId).stream()
                .map(session -> SessionResponse.fromEntity(session, enrollmentRepository.countBySessionId(session.getId())))
                .toList();
    }

    public SessionResponse findById(Long gymId, Long id) {
        TrainingSession session = getSessionOrThrow(gymId, id);
        return SessionResponse.fromEntity(session, enrollmentRepository.countBySessionId(session.getId()));
    }

    public SessionResponse create(Long gymId, SessionRequest request) {
        Gym gym = gymService.getGymOrThrow(gymId);
        Trainer trainer = getTrainerInGymOrThrow(gymId, request.trainerId());
        availabilityChecker.ensureTrainerFree(trainer.getId(), request.startTime(), request.endTime(), null);
        TrainingSession session = new TrainingSession();
        session.setGym(gym);
        applyRequest(session, trainer, request);
        TrainingSession saved = sessionRepository.save(session);
        return SessionResponse.fromEntity(saved, 0);
    }

    /** Lets a trainer reschedule their own session, or an owner/manager rearrange any session, any time. */
    public SessionResponse update(Long gymId, Long id, SessionRequest request) {
        TrainingSession session = getSessionOrThrow(gymId, id);
        Trainer trainer = getTrainerInGymOrThrow(gymId, request.trainerId());
        availabilityChecker.ensureTrainerFree(trainer.getId(), request.startTime(), request.endTime(), id);
        applyRequest(session, trainer, request);
        TrainingSession saved = sessionRepository.save(session);
        return SessionResponse.fromEntity(saved, enrollmentRepository.countBySessionId(saved.getId()));
    }

    public void delete(Long gymId, Long id) {
        TrainingSession session = getSessionOrThrow(gymId, id);
        sessionRepository.delete(session);
    }

    public List<SessionEnrollmentResponse> listAssignedMembers(Long gymId, Long sessionId) {
        TrainingSession session = getSessionOrThrow(gymId, sessionId);
        return enrollmentRepository.findAllBySessionId(session.getId()).stream()
                .map(SessionEnrollmentResponse::fromEntity)
                .toList();
    }

    public SessionEnrollmentResponse assignMember(Long gymId, Long sessionId, Long memberId) {
        TrainingSession session = getSessionOrThrow(gymId, sessionId);
        Member member = memberRepository.findByIdAndGymId(memberId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + memberId + " in gym: " + gymId));

        if (enrollmentRepository.findBySessionIdAndMemberId(sessionId, memberId).isPresent()) {
            throw new ConflictException("Member " + memberId + " is already assigned to session " + sessionId);
        }

        if (session.getCapacity() != null && enrollmentRepository.countBySessionId(sessionId) >= session.getCapacity()) {
            throw new ConflictException("Session " + sessionId + " is at full capacity");
        }

        SessionEnrollment enrollment = new SessionEnrollment();
        enrollment.setSession(session);
        enrollment.setMember(member);
        enrollment.setAssignedAt(LocalDateTime.now());
        return SessionEnrollmentResponse.fromEntity(enrollmentRepository.save(enrollment));
    }

    public void unassignMember(Long gymId, Long sessionId, Long memberId) {
        getSessionOrThrow(gymId, sessionId);
        SessionEnrollment enrollment = enrollmentRepository.findBySessionIdAndMemberId(sessionId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member " + memberId + " is not assigned to session " + sessionId));
        enrollmentRepository.delete(enrollment);
    }

    private TrainingSession getSessionOrThrow(Long gymId, Long id) {
        return sessionRepository.findByIdAndGymId(id, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id + " in gym: " + gymId));
    }

    private Trainer getTrainerInGymOrThrow(Long gymId, Long trainerId) {
        return trainerRepository.findByIdAndGymId(trainerId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found with id: " + trainerId + " in gym: " + gymId));
    }

    private void applyRequest(TrainingSession session, Trainer trainer, SessionRequest request) {
        session.setTrainer(trainer);
        session.setTitle(request.title());
        session.setDescription(request.description());
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        session.setCapacity(request.capacity());
    }
}
