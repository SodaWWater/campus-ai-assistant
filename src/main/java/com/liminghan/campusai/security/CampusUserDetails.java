package com.liminghan.campusai.security;

import com.liminghan.campusai.entity.SysUser;

/**
 * Simple UserDetails wrapper for SysUser.
 */
public class CampusUserDetails implements org.springframework.security.core.userdetails.UserDetails {

    private final SysUser user;

    public CampusUserDetails(SysUser user) {
        this.user = user;
    }

    public Long getId() {
        return user.getId();
    }

    public String getNickname() {
        return user.getNickname();
    }

    public String getRole() {
        return user.getRole();
    }

    @Override
    public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
        return java.util.List.of(
            new org.springframework.security.core.authority.SimpleGrantedAuthority(user.getRole())
        );
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
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
        return "ENABLED".equals(user.getStatus());
    }
}
