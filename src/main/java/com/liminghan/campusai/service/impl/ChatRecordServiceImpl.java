package com.liminghan.campusai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liminghan.campusai.entity.ChatRecord;
import com.liminghan.campusai.mapper.ChatRecordMapper;
import com.liminghan.campusai.service.ChatRecordService;
import org.springframework.stereotype.Service;

@Service
public class ChatRecordServiceImpl extends ServiceImpl<ChatRecordMapper, ChatRecord> implements ChatRecordService {
}
