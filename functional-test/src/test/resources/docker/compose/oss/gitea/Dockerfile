ARG IMAGE
FROM ${IMAGE}

COPY start.sh /usr/bin/custom-start.sh
COPY generate-config.sh /usr/bin/generate-config.sh
RUN chmod 755 /usr/bin/generate-config.sh
RUN chmod 755 /usr/bin/custom-start.sh
ENTRYPOINT ["/usr/bin/custom-start.sh"]