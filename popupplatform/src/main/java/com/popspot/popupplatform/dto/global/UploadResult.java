package com.popspot.popupplatform.dto.global;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadResult {
    private String url;
    private String key;
}