package io.divolte.shop.catalog;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.jackson.JsonSnakeCase;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
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
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.hibernate.validator.constraints.NotEmpty;

@Produces(MediaType.APPLICATION_JSON)
@Path("/api/catalog/item")
public class CatalogItemResource {
    private static final String ID_REGEXP = "^[0-9]{6,15}$";

    private final RestHighLevelClient client;

    public CatalogItemResource(final RestHighLevelClient client) {
        this.client = client;
    }

    @Path("{id}")
    @GET
    public void getItem(@Pattern(regexp = ID_REGEXP) @PathParam("id") final String id, @Suspended final AsyncResponse response) {
        GetRequest getRequest = new GetRequest(DataAccess.CATALOG_INDEX, DataAccess.ITEM_DOCUMENT_TYPE, id);
        client.getAsync(getRequest, new ActionListener<GetResponse>() {
            @Override
            public void onResponse(GetResponse getResponse) {
                if (getResponse.isSourceEmpty()) {
                    response.resume(Response.status(Status.NOT_FOUND).entity("Not found.").build());
                } else {
                    // We perform our own JSON parsing, because the ES
                    // JSON
                    // objects are pretty much useless API-wise.
                final Item item = DataAccess.sourceToItem(getResponse.getSourceAsString());
                response.resume(item);

                }
            }

            @Override
            public void onFailure(Exception e) {
                response.resume(e);
            }
        });
    }

    @Consumes(MediaType.APPLICATION_JSON)
    @PUT
    public void putItem(@Valid final Item item, @Suspended final AsyncResponse response) throws IOException {
        IndexRequest indexRequest = new IndexRequest(DataAccess.CATALOG_INDEX, DataAccess.ITEM_DOCUMENT_TYPE, item.id);
        indexRequest.source(itemToContentBuilder(item));
        client.indexAsync(indexRequest, new ActionListener<IndexResponse>() {
            @Override
            public void onResponse(IndexResponse indexResponse) {
                response.resume(item);
            }

            @Override
            public void onFailure(Exception e) {
                response.resume(e);
            }
        });
    }

    private XContentBuilder itemToContentBuilder(final Item item) throws IOException {
        final XContentBuilder builder = jsonBuilder();
        builder.startObject()
                .field("id", item.id)
                .field("title", item.title)
                .field("description", item.description)
                .field("tags", item.tags)
                .field("categories", item.categories)
                .field("favs", item.favs)
                .field("price", item.price)
                .startObject("owner")
                .field("user_id", item.owner.userId)
                .field("user_name", item.owner.userName)
                .field("real_name", item.owner.realName)
                .endObject()
                .startArray("variants");

        item.variants.entrySet().stream().forEach((v) -> {
            try {
                builder.startObject()
                        .field("name", v.getKey())
                        .field("flickr_url", v.getValue().flickrUrl)
                        .field("img_source", v.getValue().imgSource)
                        .field("width", v.getValue().width)
                        .field("height", v.getValue().height)
                        .endObject();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        });
        builder.endArray();
        builder.endObject();

        return builder;
    }

    @JsonSnakeCase
    public static final class Item {
        public final @NotEmpty @Pattern(regexp = ID_REGEXP) String id;
        public final @NotEmpty List<String> categories;
        public final String title;
        public final String description;
        public final List<String> tags;
        public final int favs;
        public final int price;
        public final Owner owner;
        public final Map<String, Variant> variants;

        @JsonCreator
        public Item(
                @JsonProperty("id") final String id,
                @JsonProperty("categories") final List<String> categories,
                @JsonProperty("title") final String title,
                @JsonProperty("description") final String description,
                @JsonProperty("tags") final List<String> tags,
                @JsonProperty("favs") final int favs,
                @JsonProperty("price") final int price,
                @JsonProperty("owner") final Owner owner,
                @JsonProperty("variants") final Map<String, Variant> variants) {
            this.id = id;
            this.price = price;
            this.categories = ImmutableList.copyOf(nel(categories));
            this.title = title;
            this.description = description;
            this.tags = ImmutableList.copyOf(nel(tags));
            this.favs = favs;
            this.owner = owner;
            this.variants = ImmutableMap.copyOf(nem(variants));
        }

        private static <T> List<T> nel(List<T> list) {
            return list != null ? list : Collections.emptyList();
        }

        private static <K,V> Map<K,V> nem(Map<K,V> map) {
            return map != null ? map : Collections.emptyMap();
        }

        @Override
        public String toString() {
            return "Item [id=" + id + ", categories=" + categories + ", title="
                    + title + ", description=" + description + ", tags=" + tags
                    + ", favs=" + favs + ", owner=" + owner + ", variants="
                    + variants + ", price=" + price + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((categories == null) ? 0 : categories.hashCode());
            result = prime * result
                    + ((description == null) ? 0 : description.hashCode());
            result = prime * result + favs;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            result = prime * result + ((owner == null) ? 0 : owner.hashCode());
            result = prime * result + price;
            result = prime * result + ((tags == null) ? 0 : tags.hashCode());
            result = prime * result + ((title == null) ? 0 : title.hashCode());
            result = prime * result
                    + ((variants == null) ? 0 : variants.hashCode());
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
            Item other = (Item) obj;
            if (categories == null) {
                if (other.categories != null)
                    return false;
            } else if (!categories.equals(other.categories))
                return false;
            if (description == null) {
                if (other.description != null)
                    return false;
            } else if (!description.equals(other.description))
                return false;
            if (favs != other.favs)
                return false;
            if (id == null) {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            if (owner == null) {
                if (other.owner != null)
                    return false;
            } else if (!owner.equals(other.owner))
                return false;
            if (price != other.price)
                return false;
            if (tags == null) {
                if (other.tags != null)
                    return false;
            } else if (!tags.equals(other.tags))
                return false;
            if (title == null) {
                if (other.title != null)
                    return false;
            } else if (!title.equals(other.title))
                return false;
            if (variants == null) {
                if (other.variants != null)
                    return false;
            } else if (!variants.equals(other.variants))
                return false;
            return true;
        }

        @JsonSnakeCase
        public static final class Owner {
            @NotEmpty
            public final String userName;
            @NotEmpty
            public final String userId;
            public final String realName;

            public Owner(
                    @JsonProperty("user_name") final String userName,
                    @JsonProperty("user_id") final String userId,
                    @JsonProperty("real_name") final String realName) {
                this.userName = userName;
                this.userId = userId;
                this.realName = realName;
            }

            @Override
            public String toString() {
                return "Owner [userName=" + userName + ", userId=" + userId
                        + ", realName=" + realName + "]";
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result
                        + ((realName == null) ? 0 : realName.hashCode());
                result = prime * result
                        + ((userId == null) ? 0 : userId.hashCode());
                result = prime * result
                        + ((userName == null) ? 0 : userName.hashCode());
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
                Owner other = (Owner) obj;
                if (realName == null) {
                    if (other.realName != null)
                        return false;
                } else if (!realName.equals(other.realName))
                    return false;
                if (userId == null) {
                    if (other.userId != null)
                        return false;
                } else if (!userId.equals(other.userId))
                    return false;
                if (userName == null) {
                    if (other.userName != null)
                        return false;
                } else if (!userName.equals(other.userName))
                    return false;
                return true;
            }
        }

        @JsonSnakeCase
        public static final class Variant {
            @Min(1)
            public final int width;
            @Min(1)
            public final int height;
            @NotEmpty
            public final String flickrUrl;
            @NotEmpty
            public final String imgSource;

            public Variant(
                    @JsonProperty("width") final int width,
                    @JsonProperty("height") final int height,
                    @JsonProperty("flickr_url") final String flickrUrl,
                    @JsonProperty("img_source") final String imgSource) {
                this.width = width;
                this.height = height;
                this.flickrUrl = flickrUrl;
                this.imgSource = imgSource;
            }

            @Override
            public String toString() {
                return "Variant [width=" + width + ", height=" + height
                        + ", flickrUrl=" + flickrUrl + ", imgSource="
                        + imgSource + "]";
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result
                        + ((flickrUrl == null) ? 0 : flickrUrl.hashCode());
                result = prime * result + height;
                result = prime * result
                        + ((imgSource == null) ? 0 : imgSource.hashCode());
                result = prime * result + width;
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
                Variant other = (Variant) obj;
                if (flickrUrl == null) {
                    if (other.flickrUrl != null)
                        return false;
                } else if (!flickrUrl.equals(other.flickrUrl))
                    return false;
                if (height != other.height)
                    return false;
                if (imgSource == null) {
                    if (other.imgSource != null)
                        return false;
                } else if (!imgSource.equals(other.imgSource))
                    return false;
                if (width != other.width)
                    return false;
                return true;
            }
        }
    }
}
