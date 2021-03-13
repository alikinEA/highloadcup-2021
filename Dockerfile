FROM shipilev/openjdk:11
ADD /target/highloadcup-2021-1.0-jar-with-dependencies.jar highloadcup-2021-1.0.jar
ENV JAVA_OPTS="-XX:+UseShenandoahGC -Xlog:gc -Xmx2048m -Xms2048m -server"
ENTRYPOINT exec java $JAVA_OPTS -jar highloadcup-2021-1.0.jar