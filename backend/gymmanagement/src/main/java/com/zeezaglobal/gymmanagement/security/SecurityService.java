package com.zeezaglobal.gymmanagement.security;

import com.zeezaglobal.gymmanagement.entity.ManagerAccessScope;
import com.zeezaglobal.gymmanagement.entity.Role;
import com.zeezaglobal.gymmanagement.repository.GymManagerAccessRepository;
import com.zeezaglobal.gymmanagement.repository.MemberPaymentRepository;
import com.zeezaglobal.gymmanagement.repository.PaymentRepository;
import com.zeezaglobal.gymmanagement.repository.SessionRequestRepository;
import com.zeezaglobal.gymmanagement.repository.TrainingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("security")
@RequiredArgsConstructor
public class SecurityService {

    private final TrainingSessionRepository trainingSessionRepository;
    private final SessionRequestRepository sessionRequestRepository;
    private final PaymentRepository paymentRepository;
    private final MemberPaymentRepository memberPaymentRepository;
    private final GymManagerAccessRepository gymManagerAccessRepository;

    public UserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal;
    }

    public boolean isAdmin() {
        UserPrincipal user = currentUser();
        return user != null && user.getRole() == Role.ADMIN;
    }

    /** Any authenticated staff member belonging to this gym (or an admin). */
    public boolean hasGymAccess(Long gymId) {
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        return user.getRole() == Role.ADMIN || gymId.equals(user.getGymId());
    }

    /** The gym's owner (or an admin) manages gym-level settings and the team roster; a manager only if granted SETTINGS. */
    public boolean canManageGym(Long gymId) {
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (user.getRole() == Role.OWNER && gymId.equals(user.getGymId())) {
            return true;
        }
        return user.getRole() == Role.MANAGER && hasManagerScope(user, gymId, ManagerAccessScope.SETTINGS);
    }

    /** Owner or manager of the gym (or an admin) may manage trainers/members. */
    public boolean canManageStaff(Long gymId) {
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        return user.getRole() == Role.ADMIN
                || ((user.getRole() == Role.OWNER || user.getRole() == Role.MANAGER) && gymId.equals(user.getGymId()));
    }

    /** Owner/manager of the gym, or the trainer who owns the session, may manage it (or an admin). */
    public boolean canManageSession(Long gymId, Long sessionId) {
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (!gymId.equals(user.getGymId())) {
            return false;
        }
        if (user.getRole() == Role.OWNER || user.getRole() == Role.MANAGER) {
            return true;
        }
        if (user.getRole() == Role.TRAINER && sessionId != null) {
            return trainingSessionRepository.findByIdAndGymId(sessionId, gymId)
                    .map(session -> session.getTrainer().getId().equals(user.getTrainerId()))
                    .orElse(false);
        }
        return false;
    }

    /** Owner/manager may create a session for any trainer in the gym; a trainer may only create their own. */
    public boolean canCreateSessionFor(Long gymId, Long trainerId) {
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (!gymId.equals(user.getGymId())) {
            return false;
        }
        if (user.getRole() == Role.OWNER || user.getRole() == Role.MANAGER) {
            return true;
        }
        return user.getRole() == Role.TRAINER && trainerId != null && trainerId.equals(user.getTrainerId());
    }

    /** A member may request a session in their own gym. */
    public boolean canCreateSessionRequest(Long gymId) {
        UserPrincipal user = currentUser();
        return user != null && user.getRole() == Role.MEMBER && gymId.equals(user.getGymId());
    }

    /** Owner/manager of the gym, the target trainer, or the requesting member (or an admin) may view a request. */
    public boolean canAccessSessionRequest(Long gymId, Long requestId) {
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (!gymId.equals(user.getGymId())) {
            return false;
        }
        if (user.getRole() == Role.OWNER || user.getRole() == Role.MANAGER) {
            return true;
        }
        return sessionRequestRepository.findByIdAndGymId(requestId, gymId)
                .map(request -> (user.getRole() == Role.TRAINER && request.getTrainer().getId().equals(user.getTrainerId()))
                        || (user.getRole() == Role.MEMBER && request.getMember().getId().equals(user.getMemberId())))
                .orElse(false);
    }

    /** Owner/manager of the gym, or the target trainer (or an admin) may approve/reject a request. */
    public boolean canRespondToSessionRequest(Long gymId, Long requestId) {
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (!gymId.equals(user.getGymId())) {
            return false;
        }
        if (user.getRole() == Role.OWNER || user.getRole() == Role.MANAGER) {
            return true;
        }
        if (user.getRole() == Role.TRAINER) {
            return sessionRequestRepository.findByIdAndGymId(requestId, gymId)
                    .map(request -> request.getTrainer().getId().equals(user.getTrainerId()))
                    .orElse(false);
        }
        return false;
    }

    /** The requesting member, owner/manager of the gym (or an admin) may cancel a pending request. */
    public boolean canCancelSessionRequest(Long gymId, Long requestId) {
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (!gymId.equals(user.getGymId())) {
            return false;
        }
        if (user.getRole() == Role.OWNER || user.getRole() == Role.MANAGER) {
            return true;
        }
        if (user.getRole() == Role.MEMBER) {
            return sessionRequestRepository.findByIdAndGymId(requestId, gymId)
                    .map(request -> request.getMember().getId().equals(user.getMemberId()))
                    .orElse(false);
        }
        return false;
    }

    /** Front-desk staff of the gym may check in/out any member; a member may only check themselves in/out. */
    public boolean canManageCheckIn(Long gymId, Long memberId) {
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (!gymId.equals(user.getGymId())) {
            return false;
        }
        if (user.getRole() == Role.OWNER || user.getRole() == Role.MANAGER || user.getRole() == Role.TRAINER) {
            return true;
        }
        return user.getRole() == Role.MEMBER && memberId.equals(user.getMemberId());
    }

    /** Only the gym's owner or manager (not trainers or members) may correct/force-close/delete a check-in record. */
    public boolean canManageGymCheckIns(Long gymId) {
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        return user.getRole() == Role.ADMIN
                || ((user.getRole() == Role.OWNER || user.getRole() == Role.MANAGER) && gymId.equals(user.getGymId()));
    }

    /** Only staff (owner/manager/trainer) may browse the full member directory — not a member's own token. */
    public boolean canViewMemberDirectory(Long gymId) {
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        return user.getRole() == Role.ADMIN
                || ((user.getRole() == Role.OWNER || user.getRole() == Role.MANAGER || user.getRole() == Role.TRAINER)
                        && gymId.equals(user.getGymId()));
    }

    /** Staff may view any member's record; a member may only view their own. */
    public boolean canViewMember(Long gymId, Long memberId) {
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (!gymId.equals(user.getGymId())) {
            return false;
        }
        if (user.getRole() == Role.OWNER || user.getRole() == Role.MANAGER || user.getRole() == Role.TRAINER) {
            return true;
        }
        return user.getRole() == Role.MEMBER && memberId.equals(user.getMemberId());
    }

    /** Only staff (not members) may see the gym-wide list of who's currently checked in. */
    public boolean canViewGymActivity(Long gymId) {
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        return user.getRole() == Role.ADMIN
                || ((user.getRole() == Role.OWNER || user.getRole() == Role.MANAGER || user.getRole() == Role.TRAINER)
                        && gymId.equals(user.getGymId()));
    }

    /** The gym's owner controls pay rates and payroll; a manager only if granted the FINANCE scope. */
    public boolean canManageFinance(Long gymId) {
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (user.getRole() == Role.OWNER && gymId.equals(user.getGymId())) {
            return true;
        }
        return user.getRole() == Role.MANAGER && hasManagerScope(user, gymId, ManagerAccessScope.FINANCE);
    }

    /** Owner (or admin) manages trainers freely; a manager only if granted the TRAINERS scope. */
    public boolean canManageTrainers(Long gymId) {
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (user.getRole() == Role.OWNER && gymId.equals(user.getGymId())) {
            return true;
        }
        return user.getRole() == Role.MANAGER && hasManagerScope(user, gymId, ManagerAccessScope.TRAINERS);
    }

    /** Same as canManageTrainers, plus a trainer may edit their own profile. */
    public boolean canManageTrainer(Long gymId, Long trainerId) {
        if (canManageTrainers(gymId)) {
            return true;
        }
        UserPrincipal user = currentUser();
        return user != null && user.getRole() == Role.TRAINER
                && gymId.equals(user.getGymId())
                && trainerId != null && trainerId.equals(user.getTrainerId());
    }

    private boolean hasManagerScope(UserPrincipal user, Long gymId, ManagerAccessScope scope) {
        return gymId.equals(user.getGymId())
                && user.getManagerId() != null
                && gymManagerAccessRepository.existsByGymIdAndManagerIdAndScope(gymId, user.getManagerId(), scope);
    }

    /** The owner (or admin) may view any trainer's compensation; a trainer may view only their own. */
    public boolean canViewTrainerCompensation(Long gymId, Long trainerId) {
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (!gymId.equals(user.getGymId())) {
            return false;
        }
        if (user.getRole() == Role.OWNER) {
            return true;
        }
        return user.getRole() == Role.TRAINER && trainerId.equals(user.getTrainerId());
    }

    /** The owner (or admin) may view any manager's compensation; a manager may view only their own. */
    public boolean canViewManagerCompensation(Long gymId, Long managerId) {
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (!gymId.equals(user.getGymId())) {
            return false;
        }
        if (user.getRole() == Role.OWNER) {
            return true;
        }
        return user.getRole() == Role.MANAGER && managerId.equals(user.getManagerId());
    }

    /** Any staff member (not members) may list payments in their gym — visibility is filtered to "own" in the service. */
    public boolean canListPayments(Long gymId) {
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        return user.getRole() == Role.ADMIN
                || ((user.getRole() == Role.OWNER || user.getRole() == Role.MANAGER || user.getRole() == Role.TRAINER)
                        && gymId.equals(user.getGymId()));
    }

    /** The owner (or admin) may view any payment; a trainer/manager may view only payments made to them. */
    public boolean canAccessPayment(Long gymId, Long paymentId) {
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (!gymId.equals(user.getGymId())) {
            return false;
        }
        if (user.getRole() == Role.OWNER) {
            return true;
        }
        return paymentRepository.findByIdAndGymId(paymentId, gymId)
                .map(payment -> (user.getRole() == Role.TRAINER && payment.getTrainer() != null
                        && payment.getTrainer().getId().equals(user.getTrainerId()))
                        || (user.getRole() == Role.MANAGER && payment.getManager() != null
                        && payment.getManager().getId().equals(user.getManagerId())))
                .orElse(false);
    }

    /** Owner/manager may manage any member's subscription/payments in the gym; a member may only manage their own. */
    public boolean canManageMemberBilling(Long gymId, Long memberId) {
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (!gymId.equals(user.getGymId())) {
            return false;
        }
        if (user.getRole() == Role.OWNER || user.getRole() == Role.MANAGER) {
            return true;
        }
        return user.getRole() == Role.MEMBER && memberId.equals(user.getMemberId());
    }

    /** Owner/manager may view any member payment in the gym; a member may view only their own. */
    public boolean canAccessMemberPayment(Long gymId, Long paymentId) {
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (!gymId.equals(user.getGymId())) {
            return false;
        }
        if (user.getRole() == Role.OWNER || user.getRole() == Role.MANAGER) {
            return true;
        }
        if (user.getRole() == Role.MEMBER) {
            return memberPaymentRepository.findByIdAndGymId(paymentId, gymId)
                    .map(payment -> payment.getMember().getId().equals(user.getMemberId()))
                    .orElse(false);
        }
        return false;
    }
}
