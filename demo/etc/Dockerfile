FROM openjdk:11.0.15-oracle

COPY config /nessus-aries/config
COPY bin /nessus-aries/bin
COPY lib /nessus-aries/lib

ENTRYPOINT ["/nessus-aries/bin/run.sh"]
