package io.divolte.shop;

import io.divolte.shop.catalog.BasketResource;
import io.divolte.shop.catalog.CatalogCategoryResource;
import io.divolte.shop.catalog.DataAccess;
import io.divolte.shop.catalog.CatalogItemResource;
import io.divolte.shop.catalog.CheckoutResource;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;

import org.apache.http.HttpHost;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;

import com.google.common.io.Resources;
import org.elasticsearch.common.xcontent.XContentType;

public class Main extends Application<ServiceConfiguration> {

    @Override
    public void initialize(final Bootstrap<ServiceConfiguration> bootstrap) {
    }

    @Override
    public void run(final ServiceConfiguration configuration, final Environment environment) throws Exception {
        enableCrossOriginResourceSharing(environment);

        final RestHighLevelClient client = setupElasticSearchClient(configuration);
        createIndexesIfNotExists(client);

        environment.jersey().register(new CatalogItemResource(client));
        environment.jersey().register(new CatalogCategoryResource(client));
        environment.jersey().register(new BasketResource(client));
        environment.jersey().register(new CheckoutResource());

        environment.healthChecks().register("ElasticSearch", new ElasticSearchHealthCheck(client));
    }

    private void createIndexesIfNotExists(RestHighLevelClient client) throws ElasticsearchException, IOException {
        if (client.getLowLevelClient().performRequest("HEAD", DataAccess.CATALOG_INDEX).getStatusLine().getStatusCode() != 200) {
            CreateIndexRequest createRequest = new CreateIndexRequest(DataAccess.CATALOG_INDEX);
            createRequest.settings(Settings.builder()
                    .loadFromSource(
                            Resources.toString(Resources.getResource("settings.json"), StandardCharsets.UTF_8),
                            XContentType.JSON)
                    .build()
            );
            createRequest.mapping(
                    DataAccess.ITEM_DOCUMENT_TYPE,
                    Resources.toString(Resources.getResource("mapping.json"), StandardCharsets.UTF_8),
                    XContentType.JSON
            );

            client.indices().create(createRequest);
        }
    }

    private void enableCrossOriginResourceSharing(final Environment environment) {
        Dynamic filter = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
        filter.setInitParameter("allowedOrigins", "*"); // allowed origins comma separated
        filter.setInitParameter("allowedHeaders", "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin");
        filter.setInitParameter("allowedMethods", "GET,PUT,POST,DELETE,OPTIONS,HEAD");
        filter.setInitParameter("preflightMaxAge", "5184000"); // 2 months
        filter.setInitParameter("allowCredentials", "true");
        filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    }

    private RestHighLevelClient setupElasticSearchClient(final ServiceConfiguration configuration) {

        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(configuration.esHosts
                        .stream()
                        .map((host) -> new HttpHost(host, configuration.esPort,"http"))
                        .toArray(HttpHost[]::new)));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
              client.close();
            } catch (IOException e) {
              // Do Nothing
            }
        }));
        return client;
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }
}
