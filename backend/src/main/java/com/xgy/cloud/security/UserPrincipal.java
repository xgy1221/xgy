package com.xgy.cloud.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String phone;
    private Long orgId;
    private List<String> roles;
    private String currentRole;
    private Long currentStudentId;
    private String tokenId;
    private Instant tokenExpireAt;
    private String rawToken;

    public UserPrincipal(Long userId, String phone, Long orgId, List<String> roles,
                         String currentRole, Long currentStudentId) {
        this.userId = userId;
        this.phone = phone;
        this.orgId = orgId;
        this.roles = roles != null ? roles : List.of();
        this.currentRole = currentRole;
        this.currentStudentId = currentStudentId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return phone;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
