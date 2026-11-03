package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.ClientProgressResponse;
import com.zeezaglobal.gymmanagement.entity.DailyHealthRecord;
import com.zeezaglobal.gymmanagement.entity.FoodIntakeRecord;
import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.entity.WorkoutRecord;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.DailyHealthRecordRepository;
import com.zeezaglobal.gymmanagement.repository.FoodIntakeRepository;
import com.zeezaglobal.gymmanagement.repository.MemberRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.repository.WorkoutRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Trainer view of one client's synced progress. Reads only what the client's
 * mobile app already uploaded to the database (daily health records,
 * workouts, food intake) — it adds no parallel tables for health data.
 * A client with no app account, or with no records at all, is reported as
 * such so the frontend can show the "not using the mobile app" warning
 * instead of an empty chart.
 */
@Service
@RequiredArgsConstructor
public class ClientProgressService {

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final DailyHealthRecordRepository dailyHealthRecordRepository;
    private final WorkoutRecordRepository workoutRecordRepository;
    private final FoodIntakeRepository foodIntakeRepository;
    private final GymService gymService;

    @Transactional(readOnly = true)
    public ClientProgressResponse getProgress(Long gymId, Long memberId, LocalDate from, LocalDate to) {
        gymService.getGymOrThrow(gymId);
        Member member = memberRepository.findByIdAndGymId(memberId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(6);
        if (start.isAfter(end)) {
            LocalDate swap = start;
            start = end;
            end = swap;
        }

        Optional<User> appUser = userRepository.findByMemberId(memberId);
        if (appUser.isEmpty()) {
            return new ClientProgressResponse(member.getId(), member.getFirstName(), member.getLastName(),
                    member.getEmail(), false, false, start, end, List.of(), List.of());
        }
        Long userId = appUser.get().getId();
        boolean hasAny = dailyHealthRecordRepository.existsByUserId(userId)
                || workoutRecordRepository.existsByUserId(userId)
                || foodIntakeRepository.existsByUserId(userId);

        List<DailyHealthRecord> records = dailyHealthRecordRepository
                .findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(userId, start, end);
        List<WorkoutRecord> workouts = workoutRecordRepository
                .findByUserIdAndStartTimeBetweenOrderByStartTimeDesc(userId, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        List<FoodIntakeRecord> intakes = foodIntakeRepository
                .findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(userId, start, end);

        Map<LocalDate, ClientProgressResponse.Day> byDay = new HashMap<>();
        for (DailyHealthRecord r : records) {
            byDay.put(r.getRecordDate(), dayFor(r.getRecordDate(), r, null, null, null));
        }
        Map<LocalDate, int[]> intakeSums = new HashMap<>();
        Map<LocalDate, double[]> intakeMacros = new HashMap<>();
        Map<LocalDate, List<ClientProgressResponse.IntakeEntry>> intakeByDay = new HashMap<>();
        for (FoodIntakeRecord f : intakes) {
            intakeSums.computeIfAbsent(f.getRecordDate(), k -> new int[1])[0] += f.getCalories() != null ? f.getCalories() : 0;
            double[] macros = intakeMacros.computeIfAbsent(f.getRecordDate(), k -> new double[3]);
            macros[0] += f.getProteinG() != null ? f.getProteinG() : 0;
            macros[1] += f.getCarbsG() != null ? f.getCarbsG() : 0;
            macros[2] += f.getFatG() != null ? f.getFatG() : 0;
            intakeByDay.computeIfAbsent(f.getRecordDate(), k -> new ArrayList<>())
                    .add(new ClientProgressResponse.IntakeEntry(f.getExternalId(), f.getMeal(), f.getDishName(),
                            f.getCalories(), f.getProteinG(), f.getCarbsG(), f.getFatG(),
                            f.getPhotoData() != null && f.getPhotoData().length > 0));
        }
        Map<LocalDate, int[]> workoutCounts = new HashMap<>();
        Map<LocalDate, Double> workoutCalories = new HashMap<>();
        for (WorkoutRecord w : workouts) {
            LocalDate d = w.getStartTime().toLocalDate();
            workoutCounts.computeIfAbsent(d, k -> new int[1])[0]++;
            workoutCalories.merge(d, w.getCaloriesBurned() != null ? w.getCaloriesBurned() : 0.0, Double::sum);
        }

        List<ClientProgressResponse.Day> days = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            ClientProgressResponse.Day base = byDay.get(d);
            int[] intake = intakeSums.get(d);
            double[] macros = intakeMacros.get(d);
            int[] wc = workoutCounts.get(d);
            boolean hasData = base != null || intake != null || wc != null;
            if (!hasData) {
                continue;
            }
            days.add(new ClientProgressResponse.Day(
                    d,
                    base != null ? base.steps() : null,
                    base != null ? base.activeCalories() : null,
                    base != null ? base.totalCalories() : null,
                    base != null ? base.distanceMeters() : null,
                    base != null ? base.latestHeartRateBpm() : null,
                    base != null ? base.restingHeartRateBpm() : null,
                    base != null ? base.minHeartRateBpm() : null,
                    base != null ? base.maxHeartRateBpm() : null,
                    base != null ? base.sleepDurationMinutes() : null,
                    base != null ? base.sleepStartTime() : null,
                    base != null ? base.sleepEndTime() : null,
                    base != null ? base.weightKg() : null,
                    base != null ? base.sourceDevice() : null,
                    intake != null ? intake[0] : null,
                    macros != null ? macros[0] : null,
                    macros != null ? macros[1] : null,
                    macros != null ? macros[2] : null,
                    intakeByDay.getOrDefault(d, List.of()),
                    wc != null ? wc[0] : null,
                    workoutCalories.get(d)));
        }

        List<ClientProgressResponse.Workout> workoutDtos = workouts.stream()
                .map(w -> new ClientProgressResponse.Workout(w.getTitle(), w.getStartTime(), w.getEndTime(),
                        w.getDurationMinutes(), w.getCaloriesBurned(), w.getSetCount(), w.getTotalReps(),
                        w.getTotalVolumeKg()))
                .toList();

        return new ClientProgressResponse(member.getId(), member.getFirstName(), member.getLastName(),
                member.getEmail(), true, hasAny, start, end, days, workoutDtos);
    }

    /** Raw photo bytes for one intake entry, or empty when the entry has no photo. */
    @Transactional(readOnly = true)
    public Optional<byte[]> getIntakePhoto(Long gymId, Long memberId, String externalId) {
        gymService.getGymOrThrow(gymId);
        if (!memberRepository.findByIdAndGymId(memberId, gymId).isPresent()) {
            throw new ResourceNotFoundException("Member not found");
        }
        Optional<User> appUser = userRepository.findByMemberId(memberId);
        if (appUser.isEmpty()) {
            return Optional.empty();
        }
        return foodIntakeRepository.findByUserIdAndExternalId(appUser.get().getId(), externalId)
                .map(FoodIntakeRecord::getPhotoData)
                .filter(bytes -> bytes != null && bytes.length > 0);
    }

    private ClientProgressResponse.Day dayFor(LocalDate date, DailyHealthRecord r,
                                              Integer intake, double[] macros, Integer workoutCount) {
        return new ClientProgressResponse.Day(date, r.getSteps(), r.getActiveCalories(), r.getTotalCalories(),
                r.getDistanceMeters(), r.getLatestHeartRateBpm(), r.getRestingHeartRateBpm(),
                r.getMinHeartRateBpm(), r.getMaxHeartRateBpm(), r.getSleepDurationMinutes(),
                r.getSleepStartTime(), r.getSleepEndTime(), r.getWeightKg(), r.getSourceDevice(),
                intake, macros != null ? macros[0] : null, macros != null ? macros[1] : null,
                macros != null ? macros[2] : null, List.of(), workoutCount, null);
    }
}
