package io.divolte.shop.catalog;

import io.divolte.shop.catalog.CatalogItemResource.Item.Variant;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.google.common.collect.MapMaker;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

public final class DataAccess {
    final static ConcurrentMap<String, BasketResource.Basket> BASKETS = new MapMaker().concurrencyLevel(5).initialCapacity(100).makeMap();
    final static ConcurrentMap<String, CheckoutResource.Checkout> CHECKOUTS = new MapMaker().concurrencyLevel(5).initialCapacity(100).makeMap();
    final static ConcurrentMap<String, CheckoutResource.Checkout> COMPLETED_CHECKOUTS = new MapMaker().concurrencyLevel(5).initialCapacity(100).makeMap();

    private DataAccess() {
    }

    public final static String CATALOG_INDEX = "catalog";
    public final static String ITEM_DOCUMENT_TYPE = "item";

    public static CatalogItemResource.Item sourceToItem(final String json) {
        final DocumentContext parsed = JsonPath.parse(json);
        final CatalogItemResource.Item.Owner owner = new CatalogItemResource.Item.Owner(parsed
                .read("$.owner.user_name"), parsed
                .read("$.owner.user_id"), parsed
                .read("$.owner.real_name"));

        @SuppressWarnings("unchecked")
        final Map<String, Variant> variants = ((List<Object>) parsed.read("$.variants"))
                .stream()
                .collect(
                        Collectors.toMap(
                                (o) -> (String) JsonPath.read(o, "name"),
                                (o) -> new CatalogItemResource.Item.Variant(
                                        JsonPath.read(o, "width"),
                                        JsonPath.read(o, "height"),
                                        JsonPath.read(o, "flickr_url"),
                                        JsonPath.read(o, "img_source"))
                                ));

        final CatalogItemResource.Item item = new CatalogItemResource.Item(parsed.read("$.id"),
                parsed.read("$.categories"),
                parsed.read("$.title"),
                parsed.read("$.description"),
                parsed.read("$.tags"),
                parsed.read("$.favs"),
                parsed.read("$.price"),
                owner,
                variants);
        return item;
    }
}
