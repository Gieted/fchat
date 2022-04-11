FROM openjdk:17

COPY ./* /build

WORKDIR /build

RUN ./gradlew server:installDist

COPY build/install/server /app

WORKDIR /app

CMD bin/server
