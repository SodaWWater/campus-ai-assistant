package com.liminghan.campusai.service.impl;

import com.liminghan.campusai.security.CampusUserDetails;
import com.liminghan.campusai.security.JwtUtil;
import com.liminghan.campusai.service.AuthService;
import com.liminghan.campusai.vo.LoginResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserDetailsService userDetailsService,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public LoginResponse login(String username, String password) {
        CampusUserDetails userDetails = (CampusUserDetails) userDetailsService.loadUserByUsername(username);
        String storedHash = userDetails.getPassword();

        if (!passwordEncoder.matches(password, storedHash)) {
            throw new BadCredentialsException("密码错误");
        }

        String token = jwtUtil.generateToken(
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getRole()
        );

        return new LoginResponse(
                token,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getNickname(),
                userDetails.getRole()
        );
    }
}

