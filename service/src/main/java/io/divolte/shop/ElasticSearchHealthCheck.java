package io.divolte.shop;

import io.divolte.shop.catalog.DataAccess;

import java.io.InputStream;
import java.util.Map;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;

import com.codahale.metrics.health.HealthCheck;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;

public class ElasticSearchHealthCheck extends HealthCheck {
    private final RestHighLevelClient esClient;

    public ElasticSearchHealthCheck(final RestHighLevelClient esClient) {
        this.esClient = esClient;
    }

    @Override
    protected Result check() throws Exception {
        try {
            Response response = esClient.getLowLevelClient().performRequest("GET", DataAccess.CATALOG_INDEX + "/_count");

            try (InputStream is = response.getEntity().getContent()) {
                Map<String, Object> map = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
                if ((Long) map.get("count") == 0) {
                    return Result.unhealthy("Zero items found in catalog");
                }
            }
        } catch (ElasticsearchException ee) {
            // Also occurs in case of timeout
            return Result.unhealthy(ee.getMessage());
        }

        return Result.healthy();
    }
}
