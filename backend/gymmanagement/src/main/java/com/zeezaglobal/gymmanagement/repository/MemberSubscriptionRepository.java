package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.MemberSubscription;
import com.zeezaglobal.gymmanagement.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberSubscriptionRepository extends JpaRepository<MemberSubscription, Long> {

    List<MemberSubscription> findAllByGymIdAndMemberIdOrderByStartDateDesc(Long gymId, Long memberId);

    List<MemberSubscription> findAllByGymIdAndStatus(Long gymId, SubscriptionStatus status);

    Optional<MemberSubscription> findByGymIdAndMemberIdAndStatus(Long gymId, Long memberId, SubscriptionStatus status);

    Optional<MemberSubscription> findByIdAndGymId(Long id, Long gymId);

    boolean existsByPlanIdAndStatus(Long planId, SubscriptionStatus status);

    boolean existsByPlanId(Long planId);

    List<MemberSubscription> findAllByPlanId(Long planId);

    List<MemberSubscription> findAllByPlanIdAndStatusIn(Long planId, Collection<SubscriptionStatus> statuses);

    void deleteAllByMemberId(Long memberId);
}
