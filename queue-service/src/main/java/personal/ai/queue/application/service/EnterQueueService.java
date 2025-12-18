package personal.ai.queue.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import personal.ai.queue.application.port.in.EnterQueueUseCase;
import personal.ai.queue.domain.model.QueuePosition;

/**
 * Enter Queue Service (SRP)
 * 단일 책임: 대기열 진입
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterQueueService implements EnterQueueUseCase {

    private final QueueEntryValidator queueEntryValidator;
    private final QueueEntryProcessor queueEntryProcessor;

    @Override
    public QueuePosition enter(EnterQueueCommand command) {
        String concertId = command.concertId();
        String userId = command.userId();

        // 1. 이미 진입한 사용자인지 확인 (기존 상태 반환)
        return queueEntryValidator.checkActiveUser(concertId, userId)
                .or(() -> queueEntryValidator.checkWaitingUser(concertId, userId))
                // 2. 신규 진입 처리
                .orElseGet(() -> queueEntryProcessor.proceed(concertId, userId));
    }
}
