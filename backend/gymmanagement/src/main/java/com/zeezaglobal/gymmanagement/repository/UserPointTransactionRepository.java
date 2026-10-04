package com.zeezaglobal.gymmanagement.repository;

import com.zeezaglobal.gymmanagement.entity.UserPointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserPointTransactionRepository extends JpaRepository<UserPointTransaction, String> {

    List<UserPointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<UserPointTransaction> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime after);

    @Query("SELECT COALESCE(SUM(t.points), 0) FROM UserPointTransaction t WHERE t.user.id = :userId")
    Integer calculateBalance(@Param("userId") Long userId);
}
