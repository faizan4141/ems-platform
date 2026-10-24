package com.ems.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;

@Component
public class UpstreamUnavailableFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).onErrorResume(ex -> {
            ServerHttpResponse response = exchange.getResponse();
            if (!isConnectFailure(ex) || response.isCommitted()) {
                return Mono.error(ex);   // anything else keeps the default handling
            }

            String requestId = response.getHeaders().getFirst(RequestIdFilter.HEADER);
            String body = """
                    {"status":503,"error":"Service Unavailable",\
                    "message":"The requested service is temporarily unavailable. Please retry shortly.",\
                    "path":"%s","requestId":"%s"}"""
                    .formatted(exchange.getRequest().getPath().value().replace("\"", ""), requestId);

            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            response.getHeaders().set("Retry-After", "5");
            return response.writeWith(Mono.just(
                    response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))));
        });
    }

    private static boolean isConnectFailure(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof ConnectException) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        // Right after RequestIdFilter, so it wraps every other filter and the route call.
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}