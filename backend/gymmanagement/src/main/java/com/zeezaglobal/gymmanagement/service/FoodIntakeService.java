package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.FoodIntakeSyncRequest;
import com.zeezaglobal.gymmanagement.entity.FoodIntakeRecord;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.FoodIntakeRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Mobile food-log upload. The apps keep food_log.json on-device today; this
 * endpoint lets them push entries to the database (upsert by the app's entry
 * id) so the trainer progress page can read calorie intake from the same
 * place as steps, heart rate and workouts. Until an app version uploads,
 * intake simply stays empty — it is never guessed.
 */
@Service
@RequiredArgsConstructor
public class FoodIntakeService {

    private final FoodIntakeRepository foodIntakeRepository;
    private final UserRepository userRepository;

    /** Removes one entry the app deleted locally; a no-op when it was never synced. */
    @Transactional
    public void deleteByExternalId(String externalId, UserPrincipal principal) {
        foodIntakeRepository.findByUserIdAndExternalId(principal.getUserId(), externalId)
                .ifPresent(foodIntakeRepository::delete);
    }

    @Transactional
    public int sync(FoodIntakeSyncRequest request, UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        int count = 0;
        if (request.entries() == null) {
            return count;
        }
        for (FoodIntakeSyncRequest.Entry entry : request.entries()) {
            FoodIntakeRecord record = foodIntakeRepository
                    .findByUserIdAndExternalId(user.getId(), entry.externalId())
                    .orElseGet(() -> {
                        FoodIntakeRecord created = new FoodIntakeRecord();
                        created.setUser(user);
                        created.setExternalId(entry.externalId());
                        return created;
                    });
            record.setRecordDate(entry.recordDate());
            record.setMeal(entry.meal());
            record.setDishName(entry.dishName());
            record.setCalories(entry.calories());
            record.setProteinG(entry.proteinG());
            record.setCarbsG(entry.carbsG());
            record.setFatG(entry.fatG());
            record.setUpdatedAt(LocalDateTime.now());
            foodIntakeRepository.save(record);
            count++;
        }
        return count;
    }
}
