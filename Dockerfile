FROM openjdk:16-alpine
RUN apk add --no-cache bash
COPY script.sh ./script.sh
COPY target/universal/tdr-file-export.tgz ./tdr-file-export.tgz
RUN tar -xzf ./tdr-file-export.tgz
CMD bash ./tdr-file-export/bin/tdr-file-export export --consignmentId $CONSIGNMENT_ID
