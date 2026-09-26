package io.github.weakll.mall.manager.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    String fileUpload(MultipartFile multipartFile);
}
