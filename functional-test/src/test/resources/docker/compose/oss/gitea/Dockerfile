ARG IMAGE
FROM ${IMAGE}

COPY start.sh /usr/bin/custom-start.sh
RUN chmod 755 /usr/bin/custom-start.sh
ENTRYPOINT ["/usr/bin/custom-start.sh"]