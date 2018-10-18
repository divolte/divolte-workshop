#
# Divolte Shop Demo image based on the docker-divolte image
#
# Divolte Documentation:
# www.divolte.io
#

FROM divolte/divolte-collector:latest

# Create a system account so we don't have to run with root privileges.
RUN useradd -d /opt/divolte -r divolte

ENV DIVOLTE_CONF_DIR /etc/shop/divolte

# Create the data directory and set permissions for the divolte user.
RUN mkdir -p /data/inflight && \
    chown divolte:users -R /data/ && \
    chmod -R 755 /data/

COPY show-avro /usr/local/bin/

COPY --chown=root:root \
     divolte-collector.conf \
     mapping.groovy \
     ShopEventRecord.avsc \
     /etc/shop/divolte/

USER divolte
EXPOSE 8290
