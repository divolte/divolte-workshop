package io.divolte.shop;

import io.dropwizard.Configuration;

import java.util.List;

import javax.annotation.ParametersAreNullableByDefault;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

@ParametersAreNullableByDefault
public class ServiceConfiguration extends Configuration {
    private static final String DEFAULT_CLUSTER_NAME = "elasticsearch";
    private static final List<String> DEFAULT_HOSTS = ImmutableList.of("localhost");
    private static final int DEFAULT_PORT = 9300;

    public final List<String> esHosts;
    public final int esPort;
    public final String esClusterName;

    @JsonCreator
    private ServiceConfiguration(
            @JsonProperty("elasticsearch_hosts") final String[] esHosts,
            @JsonProperty("elasticsearch_port") final int esPort,
            @JsonProperty("elasticsearch_cluster_name") final String esClusterName) {
        this.esHosts = esHosts == null ? DEFAULT_HOSTS : ImmutableList.copyOf(esHosts);
        this.esPort = esPort == 0 ? DEFAULT_PORT : esPort;
        this.esClusterName = esClusterName == null ? DEFAULT_CLUSTER_NAME : esClusterName;
    }

    public ServiceConfiguration() {
        this.esHosts = DEFAULT_HOSTS;
        this.esPort = DEFAULT_PORT;
        this.esClusterName = DEFAULT_CLUSTER_NAME;
    }
}
