package io.github.weakll.mall.manager.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix="mall.minio") //读取节点
public class MinioProperties {
    private String endpointUrl;
    private String accessKey;
    private String secreKey;
    private String bucketName;
}
