package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.PointsSyncRequest;
import com.zeezaglobal.gymmanagement.dto.PointsSyncResponse;
import com.zeezaglobal.gymmanagement.dto.PointsTransactionDto;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.entity.UserPointTransaction;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.repository.UserPointTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointsSyncService {

    private final UserPointTransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public PointsSyncResponse getPoints(User user) {
        int balance = transactionRepository.calculateBalance(user.getId());
        List<PointsTransactionDto> transactions = transactionRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toDto).toList();
        return new PointsSyncResponse(balance, transactions);
    }

    @Transactional
    public PointsSyncResponse syncPoints(User user, String deviceId, List<PointsTransactionDto> incoming) {
        if (incoming != null && !incoming.isEmpty()) {
            for (PointsTransactionDto dto : incoming) {
                if (dto.id() == null || dto.id().isBlank()) continue;

                if (!transactionRepository.existsById(dto.id())) {
                    UserPointTransaction tx = new UserPointTransaction();
                    tx.setId(dto.id());
                    tx.setUser(user);
                    tx.setPoints(dto.points());
                    tx.setReason(dto.reason() != null ? dto.reason() : "Points transaction");
                    tx.setRupeeValue(dto.rupeeValue());
                    tx.setCreatedAt(dto.createdAt() != null ? dto.createdAt() : LocalDateTime.now());
                    tx.setDeviceId(dto.deviceId() != null ? dto.deviceId() : deviceId);
                    transactionRepository.save(tx);
                }
            }
        }

        int balance = transactionRepository.calculateBalance(user.getId());
        List<PointsTransactionDto> all = transactionRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toDto).toList();

        return new PointsSyncResponse(balance, all);
    }

    @Transactional
    public PointsSyncResponse redeem(User user, String deviceId, int points) {
        if (points <= 0) {
            throw new BadRequestException("Points amount must be positive");
        }

        int currentBalance = transactionRepository.calculateBalance(user.getId());
        if (currentBalance < points) {
            throw new BadRequestException("Insufficient points balance. Current balance: " + currentBalance);
        }

        UserPointTransaction tx = new UserPointTransaction();
        tx.setId(UUID.randomUUID().toString());
        tx.setUser(user);
        tx.setPoints(-points);
        tx.setReason("Redeemed for cash");
        tx.setRupeeValue(points / 100.0);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setDeviceId(deviceId);
        transactionRepository.save(tx);

        int newBalance = currentBalance - points;
        List<PointsTransactionDto> all = transactionRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toDto).toList();

        return new PointsSyncResponse(newBalance, all);
    }

    private PointsTransactionDto toDto(UserPointTransaction tx) {
        return new PointsTransactionDto(
                tx.getId(),
                tx.getPoints(),
                tx.getReason(),
                tx.getRupeeValue(),
                tx.getCreatedAt(),
                tx.getDeviceId()
        );
    }
}
