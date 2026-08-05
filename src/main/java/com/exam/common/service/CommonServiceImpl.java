package com.exam.common.service;

import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

// 여러 도메인이 공통으로 쓰는 기능(지금은 S3 업로드) 모음. 파일 하나 S3에 올리고 URL 받는 로직은
// 어떤 도메인에서 부르든 똑같아서 여기 한 곳에 모아둠.
@Slf4j
@Service
public class CommonServiceImpl implements CommonService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket:qket-posters}")
    private String bucket;

    @Value("${cloud.aws.region.static:ap-northeast-2}")
    private String region;

    @Value("${cloud.aws.cloudfront.domain:}")
    private String cloudfrontDomain;

    public CommonServiceImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String upload(MultipartFile file, String folder) {
        try {
            String ext = file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
                    ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.'))
                    : ".jpg";
            String key = folder + "/" + UUID.randomUUID() + ext;
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
            String baseUrl = (cloudfrontDomain != null && !cloudfrontDomain.isBlank())
                    ? "https://" + cloudfrontDomain
                    : "https://" + bucket + ".s3." + region + ".amazonaws.com";
            return baseUrl + "/" + key;
        } catch (IOException e) {
            log.error("파일 업로드 실패", e);
            throw new BusinessException(ErrorCode.UPLOAD_FAILED);
        }
    }
}
