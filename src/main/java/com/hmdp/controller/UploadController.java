package com.hmdp.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;

import com.hmdp.dto.Result;

import com.hmdp.service.FileStorageService;
import com.hmdp.utils.SystemConstants;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("upload")

public class UploadController {
    @Autowired
    private FileStorageService fileStorageService;
    @PostMapping("blog")
    public Result uploadImage(@RequestParam("file") MultipartFile image) {
//        try {
//            // 获取原始文件名称
//            String originalFilename = image.getOriginalFilename();
//            // 生成新文件名
//            String fileName = createNewFileName(originalFilename);
//            // 保存文件
//            image.transferTo(new File(SystemConstants.IMAGE_UPLOAD_DIR, fileName));
//            // 返回结果
//            log.debug("文件上传成功，{}", fileName);
//            return Result.ok(fileName);
//        } catch (IOException e) {
//            throw new RuntimeException("文件上传失败", e);
//        }
        if(image == null || image.getSize() == 0){
            return Result.fail("图片素材有误，请检查");
        }
        String fileName = UUID.randomUUID().toString().replace("-","");
        String originalFilename = image.getOriginalFilename();
        String postFix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String path = null;

        try {
            path = fileStorageService.uploadImgFile("",fileName+postFix,image.getInputStream());
            log.info("文件上传地址:{}",path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        return Result.ok(path);
    }

    @GetMapping("/blog/delete")
    public Result deleteBlogImg(@RequestParam("name") String filename) {

        fileStorageService.delete(filename);
        return Result.ok();
    }

//    private String createNewFileName(String originalFilename) {
//        // 获取后缀
//        String suffix = StrUtil.subAfter(originalFilename, ".", true);
//        // 生成目录
//        String name = UUID.randomUUID().toString();
//        int hash = name.hashCode();
//        int d1 = hash & 0xF;
//        int d2 = (hash >> 4) & 0xF;
//        // 判断目录是否存在
//        File dir = new File(SystemConstants.IMAGE_UPLOAD_DIR, StrUtil.format("/blogs/{}/{}", d1, d2));
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
//        // 生成文件名
//        return StrUtil.format("/blogs/{}/{}/{}.{}", d1, d2, name, suffix);
//    }
}
