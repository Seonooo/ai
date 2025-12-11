package personal.ai.common.redis.cursor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.stereotype.Component;

/**
 * Redis Cursor 자원 관리를 담당하는 매니저
 * Cursor의 안전한 종료와 예외 처리를 캡슐화합니다.
 */
@Slf4j
@Component
public class CursorManager {

    /**
     * Cursor를 안전하게 종료합니다.
     * 종료 중 발생하는 예외는 로깅하고 무시합니다.
     *
     * @param cursor 종료할 Cursor (null 허용)
     * @param cursorName 로깅용 Cursor 이름
     */
    public void closeQuietly(Cursor<byte[]> cursor, String cursorName) {
        if (cursor != null) {
            try {
                cursor.close();
            } catch (Exception e) {
                log.warn("Failed to close cursor");
                if (log.isDebugEnabled()) {
                    log.debug("Cursor close failed: cursorName={}", cursorName, e);
                }
            }
        }
    }
}