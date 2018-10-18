#
# Divolte Shop Demo service image
#
# Divolte Documentation:
# www.divolte.io
#
FROM openjdk:8-jdk-slim as build

COPY gradle/ /src/gradle/
COPY src/ /src/src/
COPY build.gradle \
     gradlew \
     settings.gradle \
     /src/
WORKDIR /src
RUN ./gradlew build

FROM openjdk:8-jre-slim as deploy

RUN apt-get update && \
    apt-get install -y curl && \
    rm -fr /var/lib/apt/lists/*

ENV ELASTICSEARCH_HOSTS ${ELASTICSEARCH_HOSTS:-["127.0.0.1"]}
ENV ELASTICSEARCH_CLUSTER_NAME ${ELASTICSEARCH_CLUSTER_NAME:-"elasticsearch"}

RUN mkdir -p /opt/shop

COPY --from=build /src/build/libs/shop-service-0.1-SNAPSHOT-all.jar /opt/shop/shop-service.jar
COPY start.sh /opt/shop/start.sh

EXPOSE 8080
EXPOSE 8081

CMD ["/opt/shop/start.sh"]
