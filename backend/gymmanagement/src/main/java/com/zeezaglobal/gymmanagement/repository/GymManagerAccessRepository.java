package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.GymManagerAccess;
import com.zeezaglobal.gymmanagement.entity.ManagerAccessScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GymManagerAccessRepository extends JpaRepository<GymManagerAccess, Long> {

    List<GymManagerAccess> findAllByGymId(Long gymId);

    List<GymManagerAccess> findAllByGymIdAndManagerId(Long gymId, Long managerId);

    boolean existsByGymIdAndManagerIdAndScope(Long gymId, Long managerId, ManagerAccessScope scope);

    void deleteAllByGymIdAndManagerId(Long gymId, Long managerId);
}
