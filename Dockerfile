FROM openjdk:15-alpine
ARG EXPORT_VERSION
RUN apk add --no-cache bash tar && \
    apk update && \
    apk upgrade p11-kit && \
    adduser consignment-export -D
WORKDIR /home/consignment-export
USER consignment-export
COPY exporter/target/universal/tdr-consignment-export.tgz ./tdr-consignment-export.tgz
RUN tar -xzf ./tdr-consignment-export.tgz && mkdir export
ENV EXPORT_VERSION $EXPORT_VERSION
CMD bash ./tdr-consignment-export/bin/tdr-consignment-export export --consignmentId $CONSIGNMENT_ID
