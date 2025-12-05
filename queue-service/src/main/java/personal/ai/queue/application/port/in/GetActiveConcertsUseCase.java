package personal.ai.queue.application.port.in;

import java.util.List;

/**
 * Get Active Concerts Use Case
 * 활성화된 콘서트 목록 조회
 */
public interface GetActiveConcertsUseCase {

    /**
     * 활성화된 콘서트 ID 목록 조회
     * Wait Queue 또는 Active Queue에 데이터가 있는 콘서트
     * @return 콘서트 ID 리스트
     */
    List<String> getActiveConcerts();
}
