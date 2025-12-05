package personal.ai.queue.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 연장 요청
 */
public record ExtendTokenRequest(
        @NotBlank(message = "콘서트 ID는 필수입니다.")
        String concertId,

        @NotBlank(message = "사용자 ID는 필수입니다.")
        String userId
) {}
