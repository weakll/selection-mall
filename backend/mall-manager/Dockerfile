FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="weakll"
LABEL description="精选商城 - 后台管理系统"

#设置时区
ENV TZ=Asia/Shanghai

#工作目录
WORKDIR /app

#复制打包好的jar文件
COPY target/mall-manager-1.0-SNAPSHOT.jar app.jar

#暴露端口
EXPOSE 8501
#设置环境变量
ENV JAVA_OPTS="-Xms256m -Xmx512m"
#启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
