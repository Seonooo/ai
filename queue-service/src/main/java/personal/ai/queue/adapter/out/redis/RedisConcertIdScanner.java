package personal.ai.queue.adapter.out.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import personal.ai.common.redis.cursor.CursorManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Redis SCAN을 사용하여 콘서트 ID를 조회하는 스캐너
 * Queue 키 패턴을 스캔하고 콘서트 ID를 추출합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisConcertIdScanner {

    private static final int SCAN_COUNT = 100;

    private final RedisTemplate<String, String> redisTemplate;
    private final CursorManager cursorManager;

    /**
     * Redis SCAN을 사용하여 패턴에 맞는 Queue 키들을 스캔하고 콘서트 ID를 추출합니다.
     *
     * @param pattern Redis 키 패턴 (예: "queue:wait:*")
     * @param prefix 제거할 접두사 (예: "queue:wait:")
     * @return 콘서트 ID 리스트
     */
    public List<String> scanQueueConcertIds(String pattern, String prefix) {
        var concertIds = new HashSet<String>();

        redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            org.springframework.data.redis.core.Cursor<byte[]> cursor = null;

            try {
                var scanOptions = org.springframework.data.redis.core.ScanOptions.scanOptions()
                        .match(pattern)
                        .count(SCAN_COUNT)
                        .build();

                cursor = connection.scan(scanOptions);
                while (cursor.hasNext()) {
                    var key = new String(cursor.next());
                    concertIds.add(RedisKeyGenerator.extractConcertId(key, prefix));
                }

            } finally {
                cursorManager.closeQuietly(cursor, pattern + " cursor");
            }

            return null;
        });

        return new ArrayList<>(concertIds);
    }
}