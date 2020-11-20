FROM openjdk:16-alpine
RUN apk add --no-cache bash tar && \
    adduser consignment-export -D
WORKDIR /home/consignment-export
COPY exporter/target/universal/tdr-consignment-export.tgz tdr-consignment-export.tgz
RUN tar -xzf tdr-consignment-export.tgz
CMD bash tdr-consignment-export/bin/tdr-consignment-export export --consignmentId $CONSIGNMENT_ID
