package io.divolte.shop.catalog;

import static io.divolte.shop.catalog.DataAccess.BASKETS;
import static io.divolte.shop.catalog.DataAccess.CHECKOUTS;
import static io.divolte.shop.catalog.DataAccess.COMPLETED_CHECKOUTS;
import io.divolte.shop.catalog.BasketResource.Basket;
import io.dropwizard.jackson.JsonSnakeCase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

import javax.validation.constraints.Pattern;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.cage.Cage;
import com.github.cage.GCage;
import com.google.common.io.Resources;

@Produces(MediaType.APPLICATION_JSON)
@Path("/api/checkout/")
public class CheckoutResource {
    private static final Response BAD_REQUEST_BASKET_CHANGE = Response.status(Status.BAD_REQUEST).entity("Basket change during checkout.").build();
    private static final Response NOT_FOUND = Response.status(Status.NOT_FOUND).entity("Not found.").build();

    private static final String ID_REGEXP = "^[a-f0-9]{32}$";
    private static final String CO_ID_REGEXP = "^[a-f0-9]{32,60}$";

    private final static List<String> captchas;
    static {
        try {
            captchas = Resources.readLines(Resources.getResource("captchas.txt"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not load CAPTCHA list.", e);
        }
    }
    private final static Cage cage = new GCage();
    private static final SecureRandom random = new SecureRandom();

    @Path("image")
    @Produces({ "image/jpeg" })
    @GET
    public void getImage(
            @QueryParam("t") final String text,
            @Suspended final AsyncResponse response) {
        // Sync for now.
        // We could push CAPTCHA generation to a worker pool
        response.resume(cage.draw(text));
    }

    @Path("inflight/{shopperId}")
    @GET
    public void getInflightCheckout(
            @Pattern(regexp = ID_REGEXP) @PathParam("shopperId") final String shopperId,
            @Suspended final AsyncResponse response) {
        
        final Optional<Basket> basket = Optional.ofNullable(BASKETS.get(shopperId));
        final Function<String, Checkout> newCoFunc = (k) -> new Checkout(
                k + System.currentTimeMillis(),
                0,
                null,
                IntStream
                .range(0,
                       basket.map((b) -> b.items.stream().mapToInt((i) -> i.price).sum())
                             .orElse(0))
                .mapToObj((ignored) ->
                    captchas.get((random.nextInt() & Integer.MAX_VALUE) % captchas.size()))
                .toArray((s) -> new String[s]),
                basket.orElse(null),
                false);

        final Checkout checkout = CHECKOUTS.computeIfAbsent(
                shopperId,
                newCoFunc
                );

        if (!Objects.equals(checkout.basket, BASKETS.get(shopperId))) {
            // Basket changed mid-checkout. Re-create checkout and start over.
            response.resume(newCoFunc.apply(shopperId));
        } else {
            response.resume(checkout);
        }
    }

    @Path("completed/{checkoutId}")
    @GET
    public void getCompletedCheckout(
            @Pattern(regexp = CO_ID_REGEXP) @PathParam("checkoutId") final String checkoutId,
            @Suspended final AsyncResponse response) {
        final Optional<Object> checkout = Optional.ofNullable(COMPLETED_CHECKOUTS.get(checkoutId));
        response.resume(checkout.orElse(NOT_FOUND));
    }

    @Path("inflight/{shopperId}/1")
    @POST
    public void postEmail(
            @Pattern(regexp = ID_REGEXP) @PathParam("shopperId") final String shopperId,
            @FormParam("email") final String email,
            @Suspended final AsyncResponse response) {
        final Optional<Checkout> checkout = Optional.ofNullable(CHECKOUTS.get(shopperId));
        
        if (!checkout.isPresent()) {
            response.resume(NOT_FOUND);
        } else if (!Objects.equals(checkout.get().basket, BASKETS.get(shopperId))) {
            response.resume(BAD_REQUEST_BASKET_CHANGE);
        } else {
            Checkout updatedCheckout = checkout.map((co) -> new Checkout(co.id, 1, email, co.tokens, co.basket, false)).get();
            CHECKOUTS.put(shopperId, updatedCheckout);
            response.resume(updatedCheckout);
        }
    }

    @Path("inflight/{shopperId}/2")
    @POST
    public void postResponse(
            @Pattern(regexp = ID_REGEXP) @PathParam("shopperId") final String shopperId,
            @FormParam("responses") final List<String> responses,
            @Suspended final AsyncResponse response) {
        
        final Optional<Checkout> checkout = Optional.ofNullable(CHECKOUTS.get(shopperId));
        
        if (!checkout.isPresent()) {
            response.resume(NOT_FOUND);
        } else if (!Objects.equals(checkout.get().basket, BASKETS.get(shopperId))) {
            response.resume(BAD_REQUEST_BASKET_CHANGE);
        } else {
            Checkout updatedCheckout = checkout
                .map((co) -> {
                    final int[] captchaErrors = IntStream
                            .range(0, responses.size())
                            .filter((i) -> !Objects.equals(responses.get(i), co.tokens[i]))
                            .toArray();
                    if (captchaErrors.length > 0) {
                        return new Checkout(co.id, 1, co.email, co.tokens, co.basket, false);
                    } else {
                        return new Checkout(co.id, 2, co.email, co.tokens, co.basket, true);
                    }
                })
                .get();
            
            if (updatedCheckout.valid) {
                COMPLETED_CHECKOUTS.put(updatedCheckout.id, updatedCheckout);
            }
            CHECKOUTS.put(shopperId, updatedCheckout);
            
            response.resume(updatedCheckout);
        }
    }
    
    @Path("inflight/{shopperId}")
    @DELETE
    public void finalizeCheckout(
            @Pattern(regexp = ID_REGEXP) @PathParam("shopperId") final String shopperId,
            @Suspended final AsyncResponse response) {
        Optional<Object> checkout = Optional.ofNullable(CHECKOUTS.remove(shopperId));
        if (checkout.isPresent()) {
            BASKETS.remove(shopperId);
        }
        
        response.resume(checkout.orElse(NOT_FOUND));
    }

    @JsonSnakeCase
    public static class Checkout {
        public final @NotEmpty String id;
        public final int step;
        public final String email;
        public final String[] tokens;
        public final Basket basket;
        public final boolean valid;

        @JsonCreator
        public Checkout(
                @JsonProperty("id") final String id,
                @JsonProperty("step") final int step,
                @JsonProperty("email") final String email,
                @JsonProperty("tokens") final String[] tokens,
                @JsonProperty("basket") final Basket basket,
                @JsonProperty("valid") final boolean valid) {
            this.id = id;
            this.step = step;
            this.email = email;
            this.tokens = tokens;
            this.basket = basket;
            this.valid = valid;
        }

        @Override
        public String toString() {
            return "Checkout [id=" + id + ", step=" + step + ", email=" + email + ", tokens=" + Arrays.toString(tokens) + ", basket=" + basket + ", valid=" + valid + "]";
        }
    }
}
