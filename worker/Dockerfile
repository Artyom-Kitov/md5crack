FROM amazoncorretto:21 AS runtime
EXPOSE 8081
RUN mkdir /app
COPY worker/build/libs/worker-all.jar /app
WORKDIR /app
ENTRYPOINT ["java","-jar","worker-all.jar"]