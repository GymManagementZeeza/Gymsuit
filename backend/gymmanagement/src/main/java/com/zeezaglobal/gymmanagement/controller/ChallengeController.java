package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.ChallengeCreateRequest;
import com.zeezaglobal.gymmanagement.dto.ChallengeDetailDto;
import com.zeezaglobal.gymmanagement.dto.ChallengeInviteRequest;
import com.zeezaglobal.gymmanagement.dto.ChallengeJoinRequest;
import com.zeezaglobal.gymmanagement.dto.ChallengeProgressRequest;
import com.zeezaglobal.gymmanagement.dto.ChallengeSummaryDto;
import com.zeezaglobal.gymmanagement.security.SecurityService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import com.zeezaglobal.gymmanagement.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mobile/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;
    private final SecurityService securityService;

    private UserPrincipal currentUser() {
        UserPrincipal principal = securityService.currentUser();
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return principal;
    }

    @PostMapping
    public ResponseEntity<ChallengeDetailDto> create(@RequestBody ChallengeCreateRequest request) {
        ChallengeDetailDto dto = challengeService.createChallenge(request, currentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    public ResponseEntity<List<ChallengeSummaryDto>> list() {
        return ResponseEntity.ok(challengeService.listMyChallenges(currentUser()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChallengeDetailDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(challengeService.getChallengeDetail(id, currentUser()));
    }

    @PostMapping("/join")
    public ResponseEntity<ChallengeDetailDto> join(@RequestBody ChallengeJoinRequest request) {
        return ResponseEntity.ok(challengeService.joinByCode(request, currentUser()));
    }

    @PostMapping("/{id}/invites")
    public ResponseEntity<Map<String, String>> invite(@PathVariable Long id,
                                                      @RequestBody ChallengeInviteRequest request) {
        String message = challengeService.inviteParticipant(id, request, currentUser());
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/{id}/progress")
    public ResponseEntity<Map<String, String>> progress(@PathVariable Long id,
                                                       @RequestBody ChallengeProgressRequest request) {
        String message = challengeService.submitProgress(id, request, currentUser());
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Map<String, String>> leave(@PathVariable Long id) {
        String message = challengeService.leaveChallenge(id, currentUser());
        return ResponseEntity.ok(Map.of("message", message));
    }
}
