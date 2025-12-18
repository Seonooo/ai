package personal.ai.queue.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import personal.ai.queue.application.port.out.QueueRepository;
import personal.ai.queue.domain.model.QueueConfig;
import personal.ai.queue.domain.model.QueuePosition;

/**
 * 기본 대기열 진입 처리 구현체
 * Redis 대기 큐에 사용자를 추가하고 순번을 계산
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueueEntryProcessorImpl implements QueueEntryProcessor {

    private static final int POSITION_DISPLAY_OFFSET = 1;

    private final QueueRepository queueRepository;
    private final QueueConfig queueConfig;

    @Override
    public QueuePosition proceed(String concertId, String userId) {
        long position = queueRepository.addToWaitQueue(concertId, userId);
        long totalWaiting = queueRepository.getWaitQueueSize(concertId);

        log.debug("Queue entry completed: concertId={}, userId={}, position={}",
                concertId, userId, position + POSITION_DISPLAY_OFFSET);

        return QueuePosition.newEntry(
                concertId,
                userId,
                position + POSITION_DISPLAY_OFFSET,
                totalWaiting,
                queueConfig.activeMaxSize(),
                queueConfig.activationIntervalSeconds());
    }
}
