FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="weakll"
LABEL description="精选商城 - 购物车服务"

ENV TZ=Asia/Shanghai
WORKDIR /app

COPY target/service-cart-1.0-SNAPSHOT.jar app.jar

EXPOSE 8513

ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
