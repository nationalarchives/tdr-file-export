FROM openjdk:16-alpine
RUN apk add --no-cache py3-pip bash && pip3 install awscli
COPY script.sh ./script.sh
COPY target/universal/tdr-file-export.tgz ./tdr-file-export.tgz
RUN tar -xzf ./tdr-file-export.tgz
CMD bash ./tdr-file-export/bin/tdr-file-export
