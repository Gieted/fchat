FROM openjdk:17

COPY . /build/

WORKDIR /build

RUN ./gradlew server:installDist

RUN cp server/build/install/server/* /app/

WORKDIR /app

CMD bin/server
