package com.exam.common.service;

import org.springframework.web.multipart.MultipartFile;

public interface CommonService {

    // folder: S3 안에서 파일을 구분해 넣을 폴더명 (예: "posters", "profiles", "reviews")
    // 성공 시 업로드된 파일의 URL을 반환
    String upload(MultipartFile file, String folder);
}
