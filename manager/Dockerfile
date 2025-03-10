FROM amazoncorretto:21 AS runtime
EXPOSE 8080
RUN mkdir /app
COPY manager/build/libs/manager-all.jar /app
WORKDIR /app
ENTRYPOINT ["java","-jar","manager-all.jar"]