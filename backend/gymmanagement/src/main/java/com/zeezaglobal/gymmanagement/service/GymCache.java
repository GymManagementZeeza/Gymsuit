package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.config.RedisConfig;
import com.zeezaglobal.gymmanagement.dto.GymResponse;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.GymRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Read-through cache for gym profiles.
 *
 * <p>Kept as a separate bean (instead of annotations on {@link GymService#findById(Long)})
 * for two reasons: the cached value is the <em>unfiltered</em> profile, and
 * {@code GymService} applies the member role's contact-hiding <em>after</em> the cache
 * lookup — so a manager's cached response can never leak staff contacts to a member.
 * It also sidesteps the self-invocation trap ({@code @Cacheable} is a proxy, and
 * {@code this.cachedCall()} would bypass it).
 *
 * <p>Repository is injected directly to avoid a GymService ↔ GymCache circular dependency.
 */
@Service
@RequiredArgsConstructor
public class GymCache {

    private final GymRepository gymRepository;

    @Cacheable(value = RedisConfig.GYMS, key = "#gymId")
    public GymResponse get(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Gym not found with id: " + gymId));
        return GymResponse.fromEntity(gym);
    }

    @CacheEvict(value = RedisConfig.GYMS, key = "#gymId")
    public void evict(Long gymId) {
    }
}
