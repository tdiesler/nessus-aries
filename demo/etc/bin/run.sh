#!/bin/bash

PRG="$0"

# need this for relative symlinks
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG="`dirname "$PRG"`/$link"
  fi
done

# Get absolute path of the HOMEDIR
HOMEDIR="$(dirname ${PRG})/.."
HOMEDIR="$(realpath ${HOMEDIR})"

if [ -z $JAVA_OPTS ]; then
    JAVA_OPTS="-server"
fi

if [ ${JAVA_DEBUG:=false} = "true" ]; then

    JAVA_OPTS="$JAVA_OPTS -XX:+UnlockCommercialFeatures -XX:+FlightRecorder"
    JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote"
    
    if [ -n ${JAVA_DEBUG_HOST:=localhost} ]; then
        JAVA_OPTS="$JAVA_OPTS -Djava.rmi.server.hostname=$JAVA_DEBUG_HOST"
    fi

    if [ -n ${JAVA_DEBUG_PORT:=7091} ]; then
        JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.port=$JAVA_DEBUG_PORT"
        JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.rmi.port=$JAVA_DEBUG_PORT"
    fi

    if [ ${JAVA_DEBUG_SECURITY:=true} = "false" ]; then
        JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.authenticate=false"
        JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.ssl=false"
    fi
fi

# echo "JAVA_OPTS=$JAVA_OPTS"
# echo "-Dlog4j.configuration=file://${HOMEDIR}/config/log4j2.properties"

java $JAVA_OPTS -Dlog4j.configurationFile=file://${HOMEDIR}/config/log4j2.properties \
     -jar ${HOMEDIR}/lib/nessus-aries-demo-@project.version@.jar "$@" 
