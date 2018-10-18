package io.divolte.shop.catalog;

import io.divolte.shop.catalog.CatalogItemResource.Item;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

@Path("/api/catalog")
public class CatalogCategoryResource {
    private final RestHighLevelClient client;

    public CatalogCategoryResource(RestHighLevelClient client) {
        this.client = client;
    }

    @Path("category/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public void getCategory(
            @PathParam("name") final String name,
            @DefaultValue("ID") @QueryParam("order") final String order,
            @DefaultValue("0") @QueryParam("page") final int page,
            @DefaultValue("20") @QueryParam("size") final int imagesPerPage,
            @Suspended final AsyncResponse response) {


        SearchRequest searchRequest = new SearchRequest(DataAccess.CATALOG_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.constantScoreQuery(QueryBuilders.termQuery("categories", name)))
                .sort(SortBuilders.fieldSort("_id"))
                .size(imagesPerPage)
                .from(page * imagesPerPage);
        searchRequest.source(searchSourceBuilder);

        searchAsync(name, page, response, searchRequest);
    }

    @Path("search")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public void search(
            @QueryParam("query") final String query,
            @DefaultValue("ID") @QueryParam("order") final String order,
            @DefaultValue("0") @QueryParam("page") final int page,
            @DefaultValue("20") @QueryParam("size") final int imagesPerPage,
            @Suspended final AsyncResponse response) {
        String[] terms = query.split("\\b");

        SearchRequest searchRequest = new SearchRequest(DataAccess.CATALOG_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(
                QueryBuilders.constantScoreQuery(
                    QueryBuilders.boolQuery()
                            .should(QueryBuilders.termsQuery("description", terms))
                            .should(QueryBuilders.termsQuery("tags", terms))
                            .minimumShouldMatch(1)))
                .sort(SortBuilders.fieldSort("_id"))
                .size(imagesPerPage)
                .from(page * imagesPerPage);
        searchRequest.source(searchSourceBuilder);

        searchAsync("results", page, response, searchRequest);
    }

    private void searchAsync(@PathParam("name") String name, @DefaultValue("0") @QueryParam("page") int page, @Suspended AsyncResponse response, SearchRequest searchRequest) {
        client.searchAsync(searchRequest, new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(SearchResponse searchResponse) {
                if (searchResponse.getHits().totalHits == 0) {
                    response.resume(Response.status(Status.NOT_FOUND).entity("Not found.").build());
                } else {
                    final List<Item> items = StreamSupport
                            .stream(searchResponse.getHits().spliterator(), false)
                            .map(SearchHit::getSourceAsString)
                            .map(DataAccess::sourceToItem)
                            .collect(Collectors.toList());
                    response.resume(new Category(name, page, searchResponse.getHits().getHits().length, searchResponse.getHits().totalHits, items));

                }
            }

            @Override
            public void onFailure(Exception e) {
                response.resume(e);
            }
        });
    }

    public static final class Category {
        @JsonProperty("name")
        public final String name;
        @JsonProperty("page")
        public final int page;
        @JsonProperty("size")
        public final int size;
        @JsonProperty("total")
        public final long total;
        @JsonProperty("items")
        public final List<Item> items;

        @JsonCreator
        public Category(
                @JsonProperty("name") final String name,
                @JsonProperty("page") final int page,
                @JsonProperty("size") final int size,
                @JsonProperty("total") final long total,
                @JsonProperty("items") final List<Item> items) {
            this.name = name;
            this.page = page;
            this.size = size;
            this.total = total;
            this.items = ImmutableList.copyOf(items);
        }
    }
}
