package com.popspot.popupplatform.controller.global;

import com.popspot.popupplatform.dto.global.UploadResult;
import com.popspot.popupplatform.service.global.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final ObjectStorageService storage;

    @Value("${aws.s3.default-profile.key}")
    private String defaultProfileKey;

    /** 프로필 이미지 업로드: url + key 반환 */
    @PostMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResult> uploadProfile(@RequestParam("file") MultipartFile file) {
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            return ResponseEntity.badRequest().build();
        }
        UploadResult r = storage.upload("profiles", file);
        return ResponseEntity.ok(new UploadResult(r.getUrl(), r.getKey()));
    }

    @DeleteMapping("/profile")
    public ResponseEntity<Void> deleteProfile(@RequestParam("key") String key) {
        // 기본 이미지라면 삭제 금지
        if (isDefaultKey(key)) {
            return ResponseEntity.noContent().build();
        }

        storage.deleteByKey(key);
        return ResponseEntity.noContent().build();
    }

    private boolean isDefaultKey(String key) {
        if (key == null) return false;
        System.out.println(defaultProfileKey);
        return key.equals(defaultProfileKey)
                || key.equals("/" + defaultProfileKey);
    }
}
