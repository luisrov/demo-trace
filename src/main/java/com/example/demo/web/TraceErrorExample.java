package com.example.demo.web;

import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.context.ReactiveWebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/trace")
public class TraceErrorExample {

    private final WebClient.Builder webClientBuilder;

    private HttpGraphQlClient graphQlClient;

    private Tracer tracer;

    public TraceErrorExample(WebClient.Builder webClientBuilder, Tracer tracer) {
        this.webClientBuilder = webClientBuilder;
        this.tracer = tracer;
    }

    @EventListener
    public void createClientOnEvent(final ReactiveWebServerInitializedEvent event) {
        this.graphQlClient = HttpGraphQlClient.builder(webClientBuilder.baseUrl("http://localhost:" + event.getWebServer().getPort() + "/graphql").build())
                .build();
    }

    @RequestMapping(method = RequestMethod.GET)
    public Mono<Void> callGraphql() {
        return this.graphQlClient.document("{graphqlTraceId}").execute().handle((response, synchronousSink) -> {
            String originalTraceId = tracer.currentSpan().context().traceId();
            String graphqlTraceId = ((Map) response.getData()).get("graphqlTraceId").toString();
            log.info(originalTraceId + "-" + graphqlTraceId);
        });
    }

    @QueryMapping
    public Mono<String> graphqlTraceId() {
        return Mono.just(tracer.currentSpan().context().traceId());
    }

}
