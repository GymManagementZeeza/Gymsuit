package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.SessionRequestCreateRequest;
import com.zeezaglobal.gymmanagement.dto.SessionRequestRejectRequest;
import com.zeezaglobal.gymmanagement.dto.SessionRequestResponse;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.entity.Role;
import com.zeezaglobal.gymmanagement.entity.SessionEnrollment;
import com.zeezaglobal.gymmanagement.entity.SessionRequest;
import com.zeezaglobal.gymmanagement.entity.SessionRequestStatus;
import com.zeezaglobal.gymmanagement.entity.Trainer;
import com.zeezaglobal.gymmanagement.entity.TrainingSession;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.exception.ConflictException;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.MemberRepository;
import com.zeezaglobal.gymmanagement.repository.SessionEnrollmentRepository;
import com.zeezaglobal.gymmanagement.repository.SessionRequestRepository;
import com.zeezaglobal.gymmanagement.repository.TrainerRepository;
import com.zeezaglobal.gymmanagement.repository.TrainingSessionRepository;
import com.zeezaglobal.gymmanagement.security.SecurityService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionRequestService {

    private final SessionRequestRepository sessionRequestRepository;
    private final TrainingSessionRepository trainingSessionRepository;
    private final SessionEnrollmentRepository sessionEnrollmentRepository;
    private final TrainerRepository trainerRepository;
    private final MemberRepository memberRepository;
    private final GymService gymService;
    private final SecurityService securityService;
    private final SessionAvailabilityChecker availabilityChecker;

    public List<SessionRequestResponse> findVisible(Long gymId) {
        gymService.getGymOrThrow(gymId);
        UserPrincipal user = securityService.currentUser();
        if (user == null) {
            throw new BadRequestException("Not authenticated");
        }

        List<SessionRequest> requests = switch (user.getRole()) {
            case ADMIN, OWNER, MANAGER -> sessionRequestRepository.findAllByGymId(gymId);
            case TRAINER -> sessionRequestRepository.findAllByGymIdAndTrainerId(gymId, user.getTrainerId());
            case MEMBER -> sessionRequestRepository.findAllByGymIdAndMemberId(gymId, user.getMemberId());
        };

        return requests.stream().map(SessionRequestResponse::fromEntity).toList();
    }

    public SessionRequestResponse findById(Long gymId, Long id) {
        return SessionRequestResponse.fromEntity(getRequestOrThrow(gymId, id));
    }

    public SessionRequestResponse create(Long gymId, SessionRequestCreateRequest request) {
        Gym gym = gymService.getGymOrThrow(gymId);
        UserPrincipal user = securityService.currentUser();
        if (user == null || user.getRole() != Role.MEMBER) {
            throw new BadRequestException("Only a member can create a session request");
        }

        Member member = memberRepository.findByIdAndGymId(user.getMemberId(), gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + user.getMemberId() + " in gym: " + gymId));
        Trainer trainer = trainerRepository.findByIdAndGymId(request.trainerId(), gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found with id: " + request.trainerId() + " in gym: " + gymId));

        // Give the client instant feedback if the trainer is already booked then — approve() re-checks
        // in case another request for the same slot gets approved first.
        availabilityChecker.ensureTrainerFree(trainer.getId(), request.requestedStartTime(), request.requestedEndTime(), null);

        SessionRequest sessionRequest = new SessionRequest();
        sessionRequest.setGym(gym);
        sessionRequest.setMember(member);
        sessionRequest.setTrainer(trainer);
        sessionRequest.setRequestedStartTime(request.requestedStartTime());
        sessionRequest.setRequestedEndTime(request.requestedEndTime());
        sessionRequest.setNotes(request.notes());
        sessionRequest.setStatus(SessionRequestStatus.PENDING);
        sessionRequest.setCreatedAt(LocalDateTime.now());

        return SessionRequestResponse.fromEntity(sessionRequestRepository.save(sessionRequest));
    }

    public SessionRequestResponse approve(Long gymId, Long id) {
        SessionRequest sessionRequest = getRequestOrThrow(gymId, id);
        requirePending(sessionRequest);
        availabilityChecker.ensureTrainerFree(
                sessionRequest.getTrainer().getId(), sessionRequest.getRequestedStartTime(), sessionRequest.getRequestedEndTime(), null);

        TrainingSession session = new TrainingSession();
        session.setGym(sessionRequest.getGym());
        session.setTrainer(sessionRequest.getTrainer());
        session.setTitle("Session with " + sessionRequest.getTrainer().getFirstName() + " " + sessionRequest.getTrainer().getLastName());
        session.setStartTime(sessionRequest.getRequestedStartTime());
        session.setEndTime(sessionRequest.getRequestedEndTime());
        TrainingSession savedSession = trainingSessionRepository.save(session);

        SessionEnrollment enrollment = new SessionEnrollment();
        enrollment.setSession(savedSession);
        enrollment.setMember(sessionRequest.getMember());
        enrollment.setAssignedAt(LocalDateTime.now());
        sessionEnrollmentRepository.save(enrollment);

        sessionRequest.setStatus(SessionRequestStatus.APPROVED);
        sessionRequest.setRespondedAt(LocalDateTime.now());
        sessionRequest.setSession(savedSession);

        return SessionRequestResponse.fromEntity(sessionRequestRepository.save(sessionRequest));
    }

    public SessionRequestResponse reject(Long gymId, Long id, SessionRequestRejectRequest request) {
        SessionRequest sessionRequest = getRequestOrThrow(gymId, id);
        requirePending(sessionRequest);

        sessionRequest.setStatus(SessionRequestStatus.REJECTED);
        sessionRequest.setRespondedAt(LocalDateTime.now());
        sessionRequest.setResponseNote(request != null ? request.reason() : null);

        return SessionRequestResponse.fromEntity(sessionRequestRepository.save(sessionRequest));
    }

    public SessionRequestResponse cancel(Long gymId, Long id) {
        SessionRequest sessionRequest = getRequestOrThrow(gymId, id);
        requirePending(sessionRequest);

        sessionRequest.setStatus(SessionRequestStatus.CANCELLED);
        sessionRequest.setRespondedAt(LocalDateTime.now());

        return SessionRequestResponse.fromEntity(sessionRequestRepository.save(sessionRequest));
    }

    private void requirePending(SessionRequest sessionRequest) {
        if (sessionRequest.getStatus() != SessionRequestStatus.PENDING) {
            throw new ConflictException("Session request " + sessionRequest.getId() + " has already been " + sessionRequest.getStatus());
        }
    }

    private SessionRequest getRequestOrThrow(Long gymId, Long id) {
        return sessionRequestRepository.findByIdAndGymId(id, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Session request not found with id: " + id + " in gym: " + gymId));
    }
}
