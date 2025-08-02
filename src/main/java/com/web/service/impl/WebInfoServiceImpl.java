package com.web.service.impl;

import com.web.model.WebInfo;
import com.web.repository.WebInfoRepository;
import com.web.service.WebInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.NoSuchElementException;

@Service
public class WebInfoServiceImpl implements WebInfoService {

    private String imgPath = System.getProperty("user.dir") + File.separator
            + "src" + File.separator + "main" + File.separator + "resources"
            + File.separator + "static" + File.separator + "img";

    @Autowired
    private WebInfoRepository webInfoRepository;

    @Override
    @Transactional
    public WebInfo updateWebInfo(WebInfo webInfo, MultipartFile file) throws IOException {
        WebInfo newWebInfo = webInfoRepository.findFirstByOrderByIdAsc();

        if (!file.isEmpty()) {
            webInfo.setLogo(file.getOriginalFilename());
            Path path = Paths.get(imgPath + File.separator + "logo" + File.separator + file.getOriginalFilename());
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        }

        if (ObjectUtils.isEmpty(newWebInfo)) {
            return webInfoRepository.save(webInfo);
        } else {
            newWebInfo.setName(webInfo.getName());
            newWebInfo.setLogo(webInfo.getLogo());
            newWebInfo.setDescription(webInfo.getDescription());
            newWebInfo.setAddress(webInfo.getAddress());
            newWebInfo.setPhone(webInfo.getPhone());
            newWebInfo.setEmail(webInfo.getEmail());
            newWebInfo.setFreeShipping(webInfo.getFreeShipping());

            return webInfoRepository.save(newWebInfo);
        }
    }

    @Override
    @Transactional
    public WebInfo getWebInfo() {
        return webInfoRepository.findFirstByOrderByIdAsc();
    }

    @Override
    @Transactional
    public void addContactInfo(String name, String url) {
        WebInfo currentWebInfo = webInfoRepository.findFirstByOrderByIdAsc();
        currentWebInfo.getAttributes().put(name, url);
        webInfoRepository.save(currentWebInfo);
    }

    @Override
    @Transactional
    public void deleteContactInfo(String key) {
        WebInfo currentWebInfo = webInfoRepository.findFirstByOrderByIdAsc();
        currentWebInfo.getAttributes().remove(key);
        webInfoRepository.save(currentWebInfo);
    }
}
