FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="weakll"
LABEL description="精选商城 - 订单服务"

ENV TZ=Asia/Shanghai
WORKDIR /app

COPY target/service-order-1.0-SNAPSHOT.jar app.jar

EXPOSE 8514

ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
