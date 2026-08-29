package com.zeezaglobal.gymmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckInAutoCheckoutTask {

    private final CheckInService checkInService;

    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void run() {
        checkInService.autoCheckOutStale();
    }
}
