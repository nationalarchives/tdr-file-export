FROM openjdk:15-alpine
RUN apk add --no-cache bash tar && \
    apk update && \
    apk upgrade p11-kit && \
    adduser consignment-export -D
WORKDIR /home/consignment-export
USER consignment-export
COPY target/universal/tdr-consignment-export.tgz ./tdr-consignment-export.tgz
RUN tar -xzf ./tdr-consignment-export.tgz && mkdir export
## Temporarily check for taskToken to prevent disruption of export process whilst step function is updated
## If task token environment variable not present do not attempt to pass --taskToken argument as will cause error
CMD bash ./tdr-consignment-export/bin/tdr-consignment-export export --consignmentId $CONSIGNMENT_ID && \
    if [ ${TASK_TOKEN_ENV_VARIABLE} ]; then --tasktoken $TASK_TOKEN_ENV_VARIABLE; fi;
