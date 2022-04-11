FROM openjdk:17

COPY . /build/

WORKDIR /build

RUN chmod +x ./gradlew && ./gradlew server:installDist

RUN mkdir /app/ && cp -r server/build/install/server/* /app/

RUN rm -rf /build

WORKDIR /app

CMD /app/bin/server
