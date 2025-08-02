package com.web.service;

import com.web.model.WebInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface WebInfoService {
    WebInfo updateWebInfo(WebInfo webInfo, MultipartFile file) throws IOException;
    WebInfo getWebInfo();
    void addContactInfo(String name, String url);
    void deleteContactInfo(String key);

}
