package com.crewvy.common.S3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cloud.aws.s3.enabled", havingValue = "true")
public class S3Uploader {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    /**
     * S3에 파일을 업로드하고 전체 URL을 반환
     * @param file MultipartFile
     * @param dirName 업로드 경로 (예: "hotel", "room")
     * @return 업로드된 파일의 S3 URL
     */
    public String upload(MultipartFile file, String dirName) {
        String originalName = file.getOriginalFilename();
        String fileName = dirName + "/" + UUID.randomUUID() + "_" + originalName;

        try (InputStream inputStream = file.getInputStream()) {
            // 업로드 요청 구성
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();

            // S3에 파일 업로드
            s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
        } catch (IOException e) {
            throw new RuntimeException("S3 파일 업로드 실패: " + originalName, e);
        }

        // 전체 URL 반환 (정적 버킷 기준)
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + fileName;
    }

    public void delete(String imageUrl) {
        String fileName = imageUrl.split("amazonaws.com/")[1];
        s3Client.deleteObject(a -> a.bucket(bucket).key(fileName));
    }

    public void batchDelete(List<String> imageUrls) {
        // 배치로 여러 이미지를 한 번에 삭제
        imageUrls.parallelStream().forEach(this::delete);
    }
}
