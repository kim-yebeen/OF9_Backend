package com.nine.baseballdiary.backend.S3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    public PresignedUrlResponse generatePresignedUrl(Long userId, String domain, String fileName) {
        String objectKey = String.format("%s/user-%d/%s_%s", domain, userId, UUID.randomUUID(), fileName);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
               // .acl(ObjectCannedACL.PUBLIC_READ)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        String presignedUrl = presignedRequest.url().toString();
        String finalUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, objectKey);

        return new PresignedUrlResponse(presignedUrl, finalUrl);
    }

    public String uploadImageFromUrl(String imageUrl, Long kakaoId, String domain) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        String objectKey = "";
        try {
            URL url = new URL(imageUrl);
            try (InputStream inputStream = url.openStream()) {
                String uniqueFileName = UUID.randomUUID().toString() + ".jpg";
                objectKey = String.format("%s/kakao-%d/%s", domain, kakaoId, uniqueFileName);

                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        //.acl(ObjectCannedACL.PUBLIC_READ)
                        .contentType("image/jpeg")
                        .build();

                s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, inputStream.available()));
            }
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, objectKey);
        } catch (IOException e) {
            log.error("URL로부터 S3에 이미지 업로드 실패: {}", imageUrl, e);
            return null;
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        try {
            URL url = new URL(fileUrl);
            String objectKey = url.getPath().substring(1);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 파일 삭제 성공: {}", objectKey);
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", fileUrl, e);
        }
    }
}