package com.nine.baseballdiary.backend.S3;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public PresignedUrlResponse generatePresignedUrl(Long userId, String domain, String fileName) {
        String objectKey = String.format("%s/user-%d/%s_%s", domain, userId, UUID.randomUUID(), fileName);

        Date expiration = new Date();
        long expTimeMillis = expiration.getTime() + (1000 * 60 * 5); // 5분 후 만료
        expiration.setTime(expTimeMillis);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, objectKey)
                .withMethod(HttpMethod.PUT)
                .withExpiration(expiration);

        URL url = amazonS3.generatePresignedUrl(request);
        String finalUrl = amazonS3.getUrl(bucket, objectKey).toString();

        return new PresignedUrlResponse(url.toString(), finalUrl);
    }

    public String uploadImageFromUrl(String imageUrl, Long kakaoId, String domain) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        String objectKey = "";
        try {
            URL url = new URL(imageUrl);
            try (InputStream inputStream = url.openStream()) {
                ObjectMetadata metadata = new ObjectMetadata();

                String uniqueFileName = UUID.randomUUID().toString() + ".jpg";
                // ✅ S3 경로에 userId 대신 kakaoId를 사용합니다.
                objectKey = String.format("%s/kakao-%d/%s", domain, kakaoId, uniqueFileName);

                amazonS3.putObject(new PutObjectRequest(bucket, objectKey, inputStream, metadata));
            }
            return amazonS3.getUrl(bucket, objectKey).toString();
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
            // S3 URL에서 객체 키(파일 경로+이름)를 추출합니다.
            URL url = new URL(fileUrl);
            String objectKey = url.getPath().substring(1); // URL의 첫 '/'를 제거합니다.

            amazonS3.deleteObject(bucket, objectKey);
            log.info("S3 파일 삭제 성공: {}", objectKey);
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", fileUrl, e);
        }
    }
}
