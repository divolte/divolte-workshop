package io.divolte.shop;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion.Entry.Option;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@Path("/api/complete")
@Produces(MediaType.APPLICATION_JSON)
public class CompletionResource {
    private final Client esClient;

    public CompletionResource(Client esClient) {
        this.esClient = esClient;
    }

    @GET
    public void complete(@QueryParam("q") final String q, @Suspended final AsyncResponse response) {
        completionRequest(esClient, q).execute(actionListener(
                (suggestionResponse) -> response.resume(new CompletionResponse(suggestionResponse)),
                (exception) -> response.resume(exception)
                ));
    }

    public static SearchRequestBuilder completionRequest(final Client esClient, final String q) {
        return esClient.prepareSearch("suggestion").suggest(
                new SuggestBuilder().addSuggestion("suggestion",
                        SuggestBuilders.completionSuggestion("suggest")
                                .text(q)
                )
        );
//        return esClient.prepareSuggest("suggestion").addSuggestion(
//                new CompletionSuggestionBuilder("suggest")
//                        .text(q)
//                        .field("suggest")
//                );
    }

    public static final class CompletionResponse {
        @JsonProperty(required = true)
        public final List<CompletionOption> searches;

        @JsonProperty(value = "top_hits", required = true)
        public final List<CompletionOption> topHits;

        @SuppressWarnings("unchecked")
        public CompletionResponse(SearchResponse response) {
            /*
             * We request only one suggestion, so it's safe to take all
             * responses and flatMap out the options of the entries into a list.
             */
            this.searches = StreamSupport
                    .stream(response.getSuggest().spliterator(), false)
                    .flatMap((suggestions) -> suggestions.getEntries().stream())
                    .flatMap((entries) -> entries.getOptions().stream())
                    .map((option) -> new CompletionOption(option.getText().string(), null))
                    .collect(Collectors.toList());

            this.topHits = StreamSupport
                    .stream(response.getSuggest().spliterator(), false)
                    .map((s) -> (CompletionSuggestion) s)
                    .flatMap((suggestions) -> suggestions.getEntries().stream())
                    // we need to explicitly state the type here, because the
                    // signature uses a supertype that doesn't have a
                    // getPayloadAsMap()
                    .<Option> flatMap((entries) -> entries.getOptions().stream())
                    .findFirst()
                    // Casting required; it's Map's all the way down
                    .map((o) ->
                            ((List<Map<String, String>>) o.getHit().getSourceAsMap())
                                    .stream()
                                    .map((hit) -> new CompletionOption(hit.get("name"), hit.get("link")))
                                    .collect(Collectors.toList())
                    )
                    .orElseGet(Collections::emptyList);
        }

        @ParametersAreNonnullByDefault
        @JsonInclude(Include.NON_NULL)
        public static final class CompletionOption {
            @JsonProperty(value = "value", required = true)
            public final String value;

            @Nullable
            @JsonProperty("link")
            public final String link;

            public CompletionOption(final String value, @Nullable final String link) {
                this.value = value;
                this.link = link;
            }
        }
    }

    public static ActionListener<SearchResponse> actionListener(final Consumer<SearchResponse> onSuccess, final Consumer<Throwable> onFailure) {
        return new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(SearchResponse response) {
                onSuccess.accept(response);
            }

            @Override
            public void onFailure(Exception e) {
                onFailure.accept(e);
            }
        };
    }
}
