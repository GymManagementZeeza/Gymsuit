package com.zeezaglobal.gymmanagement.security;

import com.zeezaglobal.gymmanagement.entity.Role;
import com.zeezaglobal.gymmanagement.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String email;
    private final String password;
    private final Role role;
    private final Long gymId;
    private final Long trainerId;
    private final Long memberId;
    private final Long managerId;
    private final boolean enabled;

    public UserPrincipal(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.gymId = user.getGym() != null ? user.getGym().getId() : null;
        this.trainerId = user.getTrainer() != null ? user.getTrainer().getId() : null;
        this.memberId = user.getMember() != null ? user.getMember().getId() : null;
        this.managerId = user.getManager() != null ? user.getManager().getId() : null;
        this.enabled = user.isEnabled();
    }

    public Long getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    public Long getGymId() {
        return gymId;
    }

    public Long getTrainerId() {
        return trainerId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getManagerId() {
        return managerId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
