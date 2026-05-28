package com.liminghan.campusai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liminghan.campusai.entity.KbDocument;
import com.liminghan.campusai.mapper.KbDocumentMapper;
import com.liminghan.campusai.service.KbDocumentService;
import org.springframework.stereotype.Service;

@Service
public class KbDocumentServiceImpl extends ServiceImpl<KbDocumentMapper, KbDocument> implements KbDocumentService {
}
