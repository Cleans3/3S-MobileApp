package shelftap.server.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import shelftap.server.config.ShelfTapProperties;
import shelftap.server.member.MemberRepository;
import shelftap.server.web.ErrorCode;
import shelftap.server.web.Problems;
import shelftap.server.web.RateLimitFilter;
import shelftap.server.web.RateLimiter;
import shelftap.server.web.RequestSizeFilter;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Stateless bearer-token API. CSRF is off because there is no cookie session for a forged request to ride on,
 * CORS is off because no browser client exists, and every route not listed below is denied.
 */
@Configuration
class SecurityConfig {
    @Bean
    JwtDecoder jwtDecoder(JwtService jwt) {
        return jwt.decoder();
    }

    @Bean
    SecurityFilterChain api(HttpSecurity http, JsonMapper mapper, RateLimiter limiter, MemberRepository members,
                            ShelfTapProperties props) throws Exception {
        AuthenticationEntryPoint unauthenticated = (request, response, e) -> {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            Problems.write(response, mapper, ErrorCode.TOKEN_INVALID, ErrorCode.TOKEN_INVALID.status,
                    "access token missing, expired or malformed", null);
        };
        AccessDeniedHandler denied = (request, response, e) ->
                Problems.write(response, mapper, ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.status, "not allowed for this account", null);

        JwtAuthenticationConverter roles = new JwtAuthenticationConverter();
        roles.setJwtGrantedAuthoritiesConverter(jwt -> List.of(new SimpleGrantedAuthority("ROLE_" + jwt.getClaimAsString("role"))));

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000))
                        .frameOptions(f -> f.deny())
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
                .exceptionHandling(e -> e.authenticationEntryPoint(unauthenticated).accessDeniedHandler(denied))
                .oauth2ResourceServer(o -> o
                        .authenticationEntryPoint(unauthenticated)
                        .accessDeniedHandler(denied)
                        .jwt(j -> j.jwtAuthenticationConverter(roles)))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout", "/api/staff/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/cards/*/challenge", "/api/cards/*/auth").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/titles", "/api/titles/*", "/api/gate/*", "/actuator/health").permitAll()
                        .requestMatchers("/api/me/**", "/api/loans/**", "/api/reservations/**").hasRole("MEMBER")
                        .requestMatchers(HttpMethod.POST, "/api/members", "/api/cards").hasAnyRole("LIBRARIAN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/cards/*/public-key", "/api/cards/*/status").hasAnyRole("LIBRARIAN", "ADMIN")
                        .anyRequest().denyAll())
                .addFilterBefore(new RequestSizeFilter(props.maxRequestBody().toBytes(), mapper), BearerTokenAuthenticationFilter.class)
                .addFilterBefore(new RateLimitFilter(limiter, mapper), BearerTokenAuthenticationFilter.class)
                .addFilterAfter(new PasswordChangeGateFilter(members, mapper), BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}
