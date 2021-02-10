FROM openjdk:15-alpine
RUN apk add --no-cache bash tar && \
    apk update && \
    apk upgrade p11-kit && \
    adduser consignment-export -D
WORKDIR /home/consignment-export
USER consignment-export
COPY target/universal/tdr-consignment-export.tgz ./tdr-consignment-export.tgz
RUN tar -xzf ./tdr-consignment-export.tgz && mkdir export
CMD bash ./tdr-consignment-export/bin/tdr-consignment-export export --consignmentId $CONSIGNMENT_ID --tasktoken $TASK_TOKEN_ENV_VARIABLE
