package com.ems.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpstreamUnavailableFilterTest {

    private final UpstreamUnavailableFilter filter = new UpstreamUnavailableFilter();

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/tenants").build());
    }

    @Test
    void connectionFailure_becomes503() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.error(
                new RuntimeException("wrapper", new ConnectException("Connection refused"))));
        MockServerWebExchange exchange = exchange();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("5");
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("\"status\":503");
    }

    @Test
    void otherErrors_arePropagatedUnchanged() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.error(new IllegalStateException("real bug")));

        assertThatThrownBy(() -> filter.filter(exchange(), chain).block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("real bug");
    }
}