FROM adoptopenjdk/openjdk11
ADD /target/highloadcup-2021-1.0-jar-with-dependencies.jar highloadcup-2021-1.0.jar
ENV JAVA_OPTS="-XX:CompileThreshold=1 -XX:GCTimeRatio=99 -XX:+UseSerialGC -server"
ENTRYPOINT exec java $JAVA_OPTS -jar highloadcup-2021-1.0.jar