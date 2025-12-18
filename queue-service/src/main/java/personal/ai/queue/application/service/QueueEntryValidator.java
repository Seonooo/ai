package personal.ai.queue.application.service;

import personal.ai.queue.domain.model.QueuePosition;

import java.util.Optional;

/**
 * 대기열 진입 검증 인터페이스
 * 중복 진입(이미 활성 상태, 이미 대기 상태) 등을 검증하고 결과 반환
 */
public interface QueueEntryValidator {

    /**
     * 이미 활성화된 사용자인지 확인
     */
    Optional<QueuePosition> checkActiveUser(String concertId, String userId);

    /**
     * 이미 대기 중인 사용자인지 확인
     */
    Optional<QueuePosition> checkWaitingUser(String concertId, String userId);
}
