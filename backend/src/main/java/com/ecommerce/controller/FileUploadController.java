package com.ecommerce.controller;

import com.ecommerce.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
@PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
public class FileUploadController {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.access-url:/api/v1/uploads}")
    private String accessUrl;

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return Result.error("文件名无效");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        
        // 验证图片类型
        String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif"};
        boolean isValidExtension = false;
        for (String allowed : allowedExtensions) {
            if (allowed.equals(extension)) {
                isValidExtension = true;
                break;
            }
        }
        if (!isValidExtension) {
            return Result.error("只允许上传 JPG、JPEG、PNG、GIF 格式的图片");
        }

        // 验证文件大小（10MB）
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return Result.error("文件大小不能超过 10MB");
        }

        String filename = UUID.randomUUID().toString() + extension;

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            String url = accessUrl + "/" + filename;
            return Result.success(url);
        } catch (IOException e) {
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
}
