FROM openjdk:17

COPY . /build/

WORKDIR /build

RUN chmod +x ./gradlew && ./gradlew server:installDist

RUN mkdir /app/ && cp server/build/install/server/* /app/

WORKDIR /app

CMD bin/server
