FROM maven:3.8.8-sapmachine-21 AS base

FROM base AS app
LABEL description="Nyx"
ADD /src/ /app/src
ADD /certificates/ /app/certificates
ADD pom.xml /app/
RUN mkdir -p /app/data
RUN mkdir -p /app/temp

EXPOSE 8080
EXPOSE 8081
EXPOSE 9090

CMD cd /app && mvn clean compile spring-boot:run -DdataFolder=$DATA_FOLDER -DtempFolder=$TEMP_FOLDER "-Dspring.datasource.url=${DATASOURCE_URL}" "-Dspring.web.resources.static-locations=${STATIC_RESOURCES_LOCATION}"