package personal.ai.queue.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 대기열 진입 요청
 */
public record EnterQueueRequest(
        @NotBlank(message = "콘서트 ID는 필수입니다.")
        String concertId,

        @NotBlank(message = "사용자 ID는 필수입니다.")
        String userId
) {}
