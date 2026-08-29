package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.CheckInResponse;
import com.zeezaglobal.gymmanagement.dto.CheckInUpdateRequest;
import com.zeezaglobal.gymmanagement.entity.CheckIn;
import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.exception.ConflictException;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.CheckInRepository;
import com.zeezaglobal.gymmanagement.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckInService {

    /** No one stays "checked in" forever if they forget to tap out — capped at 4 hours. */
    private static final long MAX_VISIT_HOURS = 4;

    private final CheckInRepository checkInRepository;
    private final MemberRepository memberRepository;
    private final GymService gymService;

    public CheckInResponse checkIn(Long gymId, Long memberId) {
        var gym = gymService.getGymOrThrow(gymId);
        Member member = memberRepository.findByIdAndGymId(memberId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + memberId + " in gym: " + gymId));

        if (checkInRepository.findByGymIdAndMemberIdAndCheckOutTimeIsNull(gymId, memberId).isPresent()) {
            throw new ConflictException("Member " + memberId + " is already checked in");
        }

        CheckIn checkIn = new CheckIn();
        checkIn.setGym(gym);
        checkIn.setMember(member);
        checkIn.setCheckInTime(LocalDateTime.now());

        return CheckInResponse.fromEntity(checkInRepository.save(checkIn));
    }

    public CheckInResponse checkOut(Long gymId, Long memberId) {
        CheckIn checkIn = checkInRepository.findByGymIdAndMemberIdAndCheckOutTimeIsNull(gymId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member " + memberId + " has no active check-in"));

        checkIn.setCheckOutTime(LocalDateTime.now());

        return CheckInResponse.fromEntity(checkInRepository.save(checkIn));
    }

    public List<CheckInResponse> history(Long gymId, Long memberId) {
        gymService.getGymOrThrow(gymId);
        memberRepository.findByIdAndGymId(memberId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + memberId + " in gym: " + gymId));

        return checkInRepository.findAllByGymIdAndMemberIdOrderByCheckInTimeDesc(gymId, memberId).stream()
                .map(CheckInResponse::fromEntity)
                .toList();
    }

    public List<CheckInResponse> activeInGym(Long gymId) {
        gymService.getGymOrThrow(gymId);
        return checkInRepository.findAllByGymIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(gymId).stream()
                .map(CheckInResponse::fromEntity)
                .toList();
    }

    public List<CheckInResponse> listSince(Long gymId, LocalDateTime since) {
        gymService.getGymOrThrow(gymId);
        return checkInRepository.findAllByGymIdAndCheckInTimeAfterOrderByCheckInTimeAsc(gymId, since).stream()
                .map(CheckInResponse::fromEntity)
                .toList();
    }

    public CheckInResponse forceCheckOut(Long gymId, Long checkInId) {
        CheckIn checkIn = getCheckInOrThrow(gymId, checkInId);
        if (checkIn.getCheckOutTime() != null) {
            throw new ConflictException("Check-in " + checkInId + " is already checked out");
        }
        checkIn.setCheckOutTime(LocalDateTime.now());
        return CheckInResponse.fromEntity(checkInRepository.save(checkIn));
    }

    public CheckInResponse update(Long gymId, Long checkInId, CheckInUpdateRequest request) {
        CheckIn checkIn = getCheckInOrThrow(gymId, checkInId);
        if (request.checkOutTime() != null && request.checkOutTime().isBefore(request.checkInTime())) {
            throw new BadRequestException("checkOutTime cannot be before checkInTime");
        }
        checkIn.setCheckInTime(request.checkInTime());
        checkIn.setCheckOutTime(request.checkOutTime());
        return CheckInResponse.fromEntity(checkInRepository.save(checkIn));
    }

    public void delete(Long gymId, Long checkInId) {
        CheckIn checkIn = getCheckInOrThrow(gymId, checkInId);
        checkInRepository.delete(checkIn);
    }

    /** Auto-checks-out anyone still checked in past MAX_VISIT_HOURS — called by the scheduled task. */
    public void autoCheckOutStale() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(MAX_VISIT_HOURS);
        List<CheckIn> stale = checkInRepository.findAllByCheckOutTimeIsNullAndCheckInTimeBefore(cutoff);
        for (CheckIn checkIn : stale) {
            checkIn.setCheckOutTime(checkIn.getCheckInTime().plusHours(MAX_VISIT_HOURS));
        }
        checkInRepository.saveAll(stale);
    }

    private CheckIn getCheckInOrThrow(Long gymId, Long checkInId) {
        return checkInRepository.findByIdAndGymId(checkInId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Check-in not found with id: " + checkInId + " in gym: " + gymId));
    }
}
