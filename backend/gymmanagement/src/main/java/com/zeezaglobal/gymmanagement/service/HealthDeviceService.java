package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.DeviceRegistrationRequest;
import com.zeezaglobal.gymmanagement.dto.DeviceRegistrationResponse;
import com.zeezaglobal.gymmanagement.entity.HealthDevice;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.repository.HealthDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthDeviceService {

    private final HealthDeviceRepository deviceRepository;

    @Transactional
    public DeviceRegistrationResponse registerDevice(User user, DeviceRegistrationRequest request) {
        String deviceId = request.deviceId();
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId must not be blank");
        }

        LocalDateTime now = LocalDateTime.now();
        HealthDevice device = deviceRepository.findById(deviceId).orElseGet(() -> {
            HealthDevice d = new HealthDevice();
            d.setDeviceId(deviceId);
            d.setRegisteredAt(now);
            return d;
        });

        device.setUser(user);
        device.setPlatform(request.platform() != null ? request.platform().toUpperCase() : "OTHER");
        device.setDeviceType(request.deviceType() != null ? request.deviceType().toUpperCase() : "PHONE");
        device.setManufacturer(request.manufacturer());
        device.setModel(request.model());
        device.setOsVersion(request.osVersion());
        device.setAppVersion(request.appVersion());
        device.setHealthSource(request.healthSource() != null ? request.healthSource().toUpperCase() : "MANUAL_INPUT");
        device.setActive(true);
        device.setUpdatedAt(now);

        deviceRepository.save(device);
        log.info("Registered health device: {} for user: {}", deviceId, user.getId());

        return new DeviceRegistrationResponse(deviceId, true, "Device registered successfully");
    }

    @Transactional
    public void updateLastSync(String deviceId) {
        if (deviceId == null) return;
        deviceRepository.findById(deviceId).ifPresent(d -> {
            d.setLastSyncAt(LocalDateTime.now());
            deviceRepository.save(d);
        });
    }
}
