package com.liminghan.campusai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liminghan.campusai.entity.Conversation;
import com.liminghan.campusai.mapper.ConversationMapper;
import com.liminghan.campusai.service.ConversationService;
import org.springframework.stereotype.Service;

@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation> implements ConversationService {
}
