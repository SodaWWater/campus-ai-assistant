package com.liminghan.campusai.service;

import com.liminghan.campusai.dto.ChatAskRequest;
import com.liminghan.campusai.vo.ChatResponseVO;

public interface ChatService {

    ChatResponseVO ask(ChatAskRequest request);
}
