package personal.ai.queue.application.service;

import personal.ai.queue.domain.model.QueuePosition;

/**
 * 신규 대기열 진입 처리 인터페이스
 * 대기열 진입 전략을 추상화
 */
public interface QueueEntryProcessor {

    /**
     * 신규 사용자 대기열 진입 처리
     * 
     * @param concertId 콘서트 ID
     * @param userId    사용자 ID
     * @return 대기 순번 및 예상 시간 정보
     */
    QueuePosition proceed(String concertId, String userId);
}
