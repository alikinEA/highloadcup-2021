FROM shipilev/openjdk:11
ADD /target/highloadcup-2021-1.0-jar-with-dependencies.jar highloadcup-2021-1.0.jar
ENV JAVA_OPTS="-XX:CompileThreshold=1 -XX:+UseShenandoahGC -Xmx2000m -Xms2000m -server"
ENTRYPOINT exec java $JAVA_OPTS -jar highloadcup-2021-1.0.jar