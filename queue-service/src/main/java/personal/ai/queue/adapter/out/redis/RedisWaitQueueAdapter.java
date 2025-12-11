package personal.ai.queue.adapter.out.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis Wait Queue 전담 어댑터
 * Wait Queue 관련 작업만 담당합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisWaitQueueAdapter {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisConcertIdScanner concertIdScanner;

    /**
     * Wait Queue에 사용자를 추가합니다.
     * 이미 존재하는 경우 추가하지 않습니다 (중복 진입 방지).
     *
     * @param concertId 콘서트 ID
     * @param userId 사용자 ID
     * @return Wait Queue에서의 순번 (0-based), 없으면 null
     */
    public Long addToWaitQueue(String concertId, String userId) {
        String waitQueueKey = RedisKeyGenerator.waitQueueKey(concertId);
        double score = System.currentTimeMillis();

        // ZADD NX: 이미 존재하면 추가하지 않음
        Boolean added = redisTemplate.opsForZSet().addIfAbsent(waitQueueKey, userId, score);

        log.debug("Added to wait queue: concertId={}, userId={}, added={}", concertId, userId, added);

        return redisTemplate.opsForZSet().rank(waitQueueKey, userId);
    }

    /**
     * Wait Queue에서 사용자의 위치를 조회합니다.
     *
     * @param concertId 콘서트 ID
     * @param userId 사용자 ID
     * @return Wait Queue에서의 순번 (0-based), 없으면 null
     */
    public Long getWaitQueuePosition(String concertId, String userId) {
        String waitQueueKey = RedisKeyGenerator.waitQueueKey(concertId);
        return redisTemplate.opsForZSet().rank(waitQueueKey, userId);
    }

    /**
     * Wait Queue의 크기를 조회합니다.
     *
     * @param concertId 콘서트 ID
     * @return Wait Queue에 대기 중인 사용자 수
     */
    public Long getWaitQueueSize(String concertId) {
        String waitQueueKey = RedisKeyGenerator.waitQueueKey(concertId);
        return redisTemplate.opsForZSet().size(waitQueueKey);
    }

    /**
     * Wait Queue에서 score가 가장 낮은 (먼저 들어온) N명을 꺼냅니다.
     * 꺼낸 사용자들은 Wait Queue에서 제거됩니다.
     *
     * @param concertId 콘서트 ID
     * @param count 꺼낼 사용자 수
     * @return 꺼낸 사용자 ID 리스트
     */
    public List<String> popFromWaitQueue(String concertId, int count) {
        String waitQueueKey = RedisKeyGenerator.waitQueueKey(concertId);

        // ZPOPMIN: score가 가장 낮은 N개를 Pop
        Set<ZSetOperations.TypedTuple<String>> poppedUsers = redisTemplate.opsForZSet().popMin(waitQueueKey, count);

        if (poppedUsers == null || poppedUsers.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> userIds = poppedUsers.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.debug("Popped from wait queue: concertId={}, requested={}, actual={}",
                concertId, count, userIds.size());

        return userIds;
    }

    /**
     * Wait Queue에서 특정 사용자를 제거합니다.
     *
     * @param concertId 콘서트 ID
     * @param userId 사용자 ID
     */
    public void removeFromWaitQueue(String concertId, String userId) {
        String waitQueueKey = RedisKeyGenerator.waitQueueKey(concertId);
        redisTemplate.opsForZSet().remove(waitQueueKey, userId);

        log.debug("Removed from wait queue: concertId={}, userId={}", concertId, userId);
    }

    /**
     * Wait Queue에 있는 콘서트 ID 목록을 조회합니다.
     *
     * @return Wait Queue의 콘서트 ID 리스트
     */
    public List<String> getWaitQueueConcertIds() {
        return concertIdScanner.scanQueueConcertIds(
                RedisKeyGenerator.waitQueuePattern(),
                "queue:wait:"
        );
    }
}