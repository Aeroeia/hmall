    package com.hmall.gateway.filters;

    import cn.hutool.core.text.AntPathMatcher;
    import com.hmall.common.exception.UnauthorizedException;
    import com.hmall.common.utils.CollUtils;
    import com.hmall.gateway.config.AuthProperties;
    import com.hmall.gateway.utils.JwtTool;
    import lombok.RequiredArgsConstructor;
    import org.springframework.cloud.gateway.filter.GatewayFilterChain;
    import org.springframework.cloud.gateway.filter.GlobalFilter;
    import org.springframework.core.Ordered;
    import org.springframework.http.HttpHeaders;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.server.reactive.ServerHttpRequest;
    import org.springframework.http.server.reactive.ServerHttpResponse;
    import org.springframework.stereotype.Component;
    import org.springframework.web.server.ServerWebExchange;
    import reactor.core.publisher.Mono;

    import java.util.List;

    @Component
    @RequiredArgsConstructor
    public class AuthGlobalFilter implements GlobalFilter, Ordered {

        private final AuthProperties authProperties;

        private final JwtTool jwtTool;

        //路径匹配器
        private final AntPathMatcher antPathMatcher = new AntPathMatcher();

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            ServerHttpRequest request = exchange.getRequest();
            HttpHeaders headers = request.getHeaders();
            if(isExclude(request.getPath().toString())){
                return chain.filter(exchange);
            }
            List<String> authorization = headers.get("authorization");
            String token = null;
            Long userId = null;
            if(CollUtils.isNotEmpty(authorization)){
                token = authorization.get(0);
            }
            try {
                userId = jwtTool.parseToken(token);
            }
            catch (UnauthorizedException e){
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }
            String userInfo = userId.toString();
            //传递用户信息
            ServerWebExchange swe = exchange.mutate()
                    .request(builder -> builder.header("user-Info", userInfo))
                    .build();
            return chain.filter(swe);
        }

        private boolean isExclude(String path) {
            for (String excludePath : authProperties.getExcludePaths()) {
                if(antPathMatcher.match(excludePath, path)){
                    return true;
                }
            }
            return false;

        }

        @Override
        public int getOrder() {
            return 0;
        }
    }
