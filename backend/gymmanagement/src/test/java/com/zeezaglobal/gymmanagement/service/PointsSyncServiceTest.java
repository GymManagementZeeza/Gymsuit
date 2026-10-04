package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.PointsSyncResponse;
import com.zeezaglobal.gymmanagement.dto.PointsTransactionDto;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.entity.UserPointTransaction;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.repository.UserPointTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointsSyncServiceTest {

    @Mock
    private UserPointTransactionRepository transactionRepository;

    @InjectMocks
    private PointsSyncService pointsSyncService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("athul.6560@gmail.com");
    }

    @Test
    void testSyncPointsInsertsNewTransactionsAndCalculatesBalance() {
        PointsTransactionDto tx1 = new PointsTransactionDto(
                "tx-1", 50, "Workout: Bench Press", null, LocalDateTime.now(), "IOS_APP");
        PointsTransactionDto tx2 = new PointsTransactionDto(
                "tx-2", 75, "Workout: Squat", null, LocalDateTime.now(), "ANDROID_APP");

        when(transactionRepository.existsById("tx-1")).thenReturn(false);
        when(transactionRepository.existsById("tx-2")).thenReturn(false);
        when(transactionRepository.calculateBalance(1L)).thenReturn(125);

        PointsSyncResponse response = pointsSyncService.syncPoints(testUser, "IOS_APP", List.of(tx1, tx2));

        assertEquals(125, response.balance());
        verify(transactionRepository, times(2)).save(any(UserPointTransaction.class));
    }

    @Test
    void testSyncPointsDoesNotDuplicateExistingTransaction() {
        PointsTransactionDto tx1 = new PointsTransactionDto(
                "tx-duplicate", 50, "Workout: Bench Press", null, LocalDateTime.now(), "IOS_APP");

        when(transactionRepository.existsById("tx-duplicate")).thenReturn(true);
        when(transactionRepository.calculateBalance(1L)).thenReturn(50);

        PointsSyncResponse response = pointsSyncService.syncPoints(testUser, "ANDROID_APP", List.of(tx1));

        assertEquals(50, response.balance());
        verify(transactionRepository, never()).save(any(UserPointTransaction.class));
    }

    @Test
    void testRedeemPointsDeductsBalance() {
        when(transactionRepository.calculateBalance(1L)).thenReturn(500);

        PointsSyncResponse response = pointsSyncService.redeem(testUser, "IOS_APP", 200);

        assertEquals(300, response.balance());
        verify(transactionRepository, times(1)).save(any(UserPointTransaction.class));
    }

    @Test
    void testRedeemPointsThrowsWhenInsufficientBalance() {
        when(transactionRepository.calculateBalance(1L)).thenReturn(100);

        assertThrows(BadRequestException.class, () -> {
            pointsSyncService.redeem(testUser, "IOS_APP", 200);
        });
    }
}
