package io.github.weakll.mall.manager.service.impl;

import cn.hutool.core.date.DateUtil;
import io.github.weakll.mall.manager.properties.MinioProperties;
import io.github.weakll.mall.manager.service.FileUploadService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService {
    @Autowired
    private MinioProperties minioProperties;

    @Override
    public String fileUpload(MultipartFile multipartFile) {
        try {
            // 创建⼀个Minio的客⼾端对象
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(minioProperties.getEndpointUrl())
                    .credentials(minioProperties.getAccessKey(), minioProperties.getSecreKey())
                    .build();
            // 判断桶是否存在
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build());
            if (!found) { // 如果不存在，那么此时就创建⼀个新的桶
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build());
            } else { // 如果存在打印信息
                System.out.println("Bucket 'mall-bucket' already exists.");
            }
            // 设置存储对象名称
            String dateDir = DateUtil.format(new Date(), "yyyyMMdd");
            String uuid = UUID.randomUUID().toString().replace("-", "");
            //20230801/443e1e772bef482c95be28704bec58a901.jpg

            String fileName = dateDir + "/" + uuid + multipartFile.getOriginalFilename();
            System.out.println(fileName);
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .stream(multipartFile.getInputStream(), multipartFile.getSize(), -1)
                    .object(fileName)
                    .build();
            minioClient.putObject(putObjectArgs);
            return minioProperties.getEndpointUrl() + "/" + minioProperties.getBucketName() + "/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
