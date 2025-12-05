package personal.ai.queue.adapter.in.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import personal.ai.queue.application.config.QueueConfigProperties;

import java.io.IOException;
import java.time.Duration;

/**
 * Rate Limit Filter
 * Redis 기반 Token Bucket 알고리즘으로 폴링 요청 제한
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:queue:";
    private static final String SUBSCRIBE_PATH = "/api/v1/queue/subscribe";
    private final RedisTemplate<String, String> redisTemplate;
    private final QueueConfigProperties configProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // SSE subscribe 엔드포인트만 Rate Limiting 적용
        if (!SUBSCRIBE_PATH.equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String concertId = request.getParameter("concertId");
        String userId = request.getParameter("userId");

        if (concertId == null || userId == null) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.getWriter().write("Missing concertId or userId parameter");
            return;
        }

        // Rate Limit 체크
        if (!checkRateLimit(concertId, userId)) {
            log.warn("Rate limit exceeded: concertId={}, userId={}", concertId, userId);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After",
                    String.valueOf(configProperties.polling().rateLimitRefillDurationSeconds()));
            response.getWriter().write("Too many requests. Please slow down.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Token Bucket 알고리즘으로 Rate Limit 체크
     *
     * @return true: 허용, false: 제한
     */
    private boolean checkRateLimit(String concertId, String userId) {
        String key = RATE_LIMIT_KEY_PREFIX + concertId + ":" + userId;
        QueueConfigProperties.Polling pollingConfig = configProperties.polling();

        try {
            // 현재 토큰 개수 조회
            String currentTokensStr = redisTemplate.opsForValue().get(key);

            // 첫 요청: 초기 토큰 설정
            if (currentTokensStr == null) {
                int initialTokens = pollingConfig.rateLimitCapacity() - 1;
                redisTemplate.opsForValue().set(
                        key,
                        String.valueOf(initialTokens),
                        Duration.ofSeconds(pollingConfig.rateLimitRefillDurationSeconds())
                );

                log.debug("Rate limit initialized: concertId={}, userId={}, remaining={}",
                        concertId, userId, initialTokens);

                return true;
            }

            // 기존 토큰 확인
            int currentTokens = Integer.parseInt(currentTokensStr);

            // 토큰이 있으면 소비
            if (currentTokens > 0) {
                Long newTokens = redisTemplate.opsForValue().decrement(key);

                log.debug("Rate limit token consumed: concertId={}, userId={}, remaining={}",
                        concertId, userId, newTokens);

                return true;
            }

            // 토큰이 없으면 거부
            log.debug("Rate limit exceeded: concertId={}, userId={}, tokens=0", concertId, userId);
            return false;

        } catch (Exception e) {
            log.error("Rate limit check failed: concertId={}, userId={}", concertId, userId, e);
            // 에러 발생 시 안전하게 허용 (Fail-open)
            return true;
        }
    }
}
