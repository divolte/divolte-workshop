package io.divolte.shop.catalog;

import static io.divolte.shop.catalog.DataAccess.BASKETS;
import io.divolte.shop.catalog.CatalogItemResource.Item;
import io.dropwizard.jackson.JsonSnakeCase;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;

@Produces(MediaType.APPLICATION_JSON)
@Path("/api/basket/")
public class BasketResource {
    private static final String ID_REGEXP = "^[a-f0-9]{32}$";
    private final RestHighLevelClient client;

    public BasketResource(final RestHighLevelClient client) {
        this.client = client;
    }

    @Path("{id}")
    @GET
    public void getBasket(@Pattern(regexp = ID_REGEXP) @PathParam("id") final String id, @Suspended final AsyncResponse response) {
        final Basket basket = BASKETS.computeIfAbsent(id, (x) -> new Basket(id, Collections.emptySet()));
        response.resume(basket);
    }

    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("{id}")
    @PUT
    public void putBasketItem(@Pattern(regexp = ID_REGEXP) @PathParam("id") final String basketId, @FormParam("item_id") final String itemId, @Suspended final AsyncResponse response) {
        final Set<Item> items = Optional
                .ofNullable(BASKETS.get(basketId))
                .map((b) -> new HashSet<Item>(b.items))
                .orElseGet(() -> new HashSet<Item>());

        GetRequest getRequest = new GetRequest(DataAccess.CATALOG_INDEX, DataAccess.ITEM_DOCUMENT_TYPE, itemId);
        client.getAsync(getRequest, new ActionListener<GetResponse>() {
            @Override
            public void onResponse(GetResponse getResponse) {
                if (getResponse.isSourceEmpty()) {
                    response.resume(Response.status(Status.NOT_FOUND).entity("Item not found.").build());
                } else {
                    final Item item = DataAccess.sourceToItem(getResponse.getSourceAsString());
                    items.add(item);
                    final Basket basket = new Basket(basketId, items);
                    BASKETS.put(basketId, basket);
                    response.resume(basket);

                }
            }

            @Override
            public void onFailure(Exception e) {
                response.resume(e);
            }
        });

    }

    @Path("{basketId}/{itemId}")
    @DELETE
    public void deleteBasketItem(
            @Pattern(regexp = ID_REGEXP) @PathParam("basketId") final String basketId,
            @PathParam("itemId") final String itemId,
            @Suspended final AsyncResponse response
            ) {

        final Set<Item> items = Optional
                .ofNullable(BASKETS.get(basketId))
                .map((b) -> new HashSet<Item>(b.items))
                .orElseGet(() -> new HashSet<Item>());

        Optional<Item> item = items.stream().filter((i) -> i.id.equals(itemId)).findFirst();
        if (item.isPresent()) {
            items.remove(item.get());
            final Basket basket = new Basket(basketId, items);
            BASKETS.put(basketId, basket);
            response.resume(basket);
        } else {
            response.resume(Response.status(Status.NOT_FOUND).entity("Item not found.").build());
        }
    }

    @JsonSnakeCase
    public static class Basket {
        public final @NotEmpty @Pattern(regexp = ID_REGEXP) String id;
        public final @NotEmpty Set<Item> items;

        @JsonCreator
        public Basket(
                @JsonProperty("id") String id,
                @JsonProperty("items") Set<Item> items) {
            this.id = id;
            this.items = ImmutableSet.copyOf(items);
        }

        @Override
        public String toString() {
            return "Basket [id=" + id + ", items=" + items + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            result = prime * result + ((items == null) ? 0 : items.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Basket other = (Basket) obj;
            if (id == null) {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            if (items == null) {
                if (other.items != null)
                    return false;
            } else if (!items.equals(other.items))
                return false;
            return true;
        }
    }
}
