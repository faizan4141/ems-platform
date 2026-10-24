package com.ems.gateway.filter;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void replacesClientSuppliedId_andEchoesNewIdInResponse() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users")
                        .header(RequestIdFilter.HEADER, "attacker-controlled")
                        .build());

        filter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        String forwardedId = captor.getValue().getRequest().getHeaders().getFirst(RequestIdFilter.HEADER);

        assertThat(forwardedId).isNotEqualTo("attacker-controlled");
        assert forwardedId != null;
        assertThat(UUID.fromString(forwardedId)).isNotNull();
        assertThat(exchange.getResponse().getHeaders().getFirst(RequestIdFilter.HEADER))
                .isEqualTo(forwardedId);
    }
}