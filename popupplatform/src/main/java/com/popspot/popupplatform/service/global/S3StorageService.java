package com.popspot.popupplatform.service.global;

import com.popspot.popupplatform.dto.global.UploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3StorageService implements ObjectStorageService {

    private final S3Client s3;

    @Value("${aws.s3.bucket}") private String bucket;
    @Value("${aws.s3.public-base-url:}") private String publicBaseUrl;

    @Override
    public UploadResult upload(String keyPrefix, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("file is empty");

        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains(".")) ?
                original.substring(original.lastIndexOf('.') + 1) : "bin";

        LocalDate d = LocalDate.now();
        String key = String.format("%s/%04d/%02d/%02d/%s.%s",
                clean(keyPrefix), d.getYear(), d.getMonthValue(), d.getDayOfMonth(),
                UUID.randomUUID(), ext);

        String contentType = StringUtils.hasText(file.getContentType()) ?
                file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file content", e);
        } catch (S3Exception e) {
            throw new RuntimeException("S3 upload failed: " + e.awsErrorDetails().errorMessage(), e);
        }

        String url = buildPublicUrl(key);
        return new UploadResult(url, key);
    }

    @Override
    public void deleteByKey(String key) {
        if (!StringUtils.hasText(key)) return;
        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (S3Exception e) {
            throw new RuntimeException("S3 delete failed: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    private String clean(String p) {
        if (!StringUtils.hasText(p)) return "uploads";
        return p.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private String buildPublicUrl(String key) {
        // 1) 직접 지정한 publicBaseUrl이 있으면 그걸 최우선 사용
        if (StringUtils.hasText(publicBaseUrl)) {
            String encKey = URLEncoder.encode(key, StandardCharsets.UTF_8).replace("+", "%20");
            return publicBaseUrl.endsWith("/") ? publicBaseUrl + encKey : publicBaseUrl + "/" + encKey;
        }

        // 2) SDK가 제공하는 Utilities API로 URL 생성 (리전/주소 스타일 알아서 처리)
        return s3.utilities()
                .getUrl(GetUrlRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build())
                .toExternalForm(); // or .toString()
    }
}
