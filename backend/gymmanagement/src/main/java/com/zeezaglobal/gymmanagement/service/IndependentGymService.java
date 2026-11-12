package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.repository.GymRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The built-in gym for members who don't belong to a real gym. Every member record needs a gym, so people
 * who sign up in the app without choosing one, or join through a standalone nutritionist's invite, go here
 * instead of into some arbitrary real gym's member list.
 */
@Service
@RequiredArgsConstructor
public class IndependentGymService {

    static final String NAME = "GymSuit Independent";
    static final String EMAIL = "independent@gymsuit.app";

    private final GymRepository gymRepository;

    @Transactional
    public synchronized Gym get() {
        return gymRepository.findFirstByIndependentTrueOrderByIdAsc().orElseGet(() -> {
            Gym gym = new Gym();
            gym.setName(NAME);
            gym.setEmail(EMAIL);
            gym.setIndependent(true);
            return gymRepository.save(gym);
        });
    }
}
