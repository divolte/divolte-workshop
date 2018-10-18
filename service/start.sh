#!/bin/bash

echo -n "{
  \"elasticsearch_hosts\" : ${ELASTICSEARCH_HOSTS},
  \"elasticsearch_port\": 9200,
  \"elasticsearch_cluster_name\": \"${ELASTICSEARCH_CLUSTER_NAME}\"
}" > /opt/shop/config.json

ES_HOSTS=${ELASTICSEARCH_HOSTS//[\[\"\]]/}
ES_HOSTS_ARR=( ${ES_HOSTS//,/ } )

WAIT_LOOPS=25

is_ready() {
  [ $(curl --write-out '%{http_code}' --silent --output /dev/null http://${ES_HOSTS_ARR[0]}:9200/_cat/health?h=st) -eq 200 ]
}

i=0
while ! is_ready; do
    i=`expr $i + 1`
    if [ $i -ge ${WAIT_LOOPS} ]; then
        echo "$(date) - still not ready, giving up"
        exit 1
    fi
    echo "$(date) - waiting for elasticsearch to be ready on ${ES_HOSTS_ARR[0]}."
    sleep 5
done

java $JAVA_OPTS -jar /opt/shop/shop-service.jar server /opt/shop/config.json
