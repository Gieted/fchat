FROM openjdk:17

COPY ./* /build

WORKDIR /build

./gradlew server:installDist

COPY build/install/server /app

WORKDIR /app

CMD bin/server
