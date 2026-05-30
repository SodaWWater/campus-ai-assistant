package com.liminghan.campusai.service;

import com.liminghan.campusai.vo.LoginResponse;

public interface AuthService {

    LoginResponse login(String username, String password);
}
