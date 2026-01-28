package se.fastighet.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import se.fastighet.core.entity.User;
import se.fastighet.core.repository.UserRepository;
import se.fastighet.core.service.JwtService;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            log.debug("Invalid JWT token");
            sendUnauthorizedResponse(response, "Ogiltig eller utgången token");
            return;
        }

        try {
            UUID userId = jwtService.extractUserId(token);

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("User not found for JWT token, userId: {}", userId);
                sendUnauthorizedResponse(response, "Användare hittades inte");
                return;
            }

            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
            );

            UserPrincipal principal = new UserPrincipal(user);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT authenticated user: {} with role: {}", user.getEmail(), user.getRole());

        } catch (Exception e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
            sendUnauthorizedResponse(response, "Ogiltig eller utgången token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
                "{\"status\": 401, \"message\": \"%s\"}", message
        ));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/health") ||
               path.equals("/api/auth/login") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/api-docs");
    }
}
