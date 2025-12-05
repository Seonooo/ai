package personal.ai.queue.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 검증 요청
 */
public record ValidateTokenRequest(
        @NotBlank(message = "콘서트 ID는 필수입니다.")
        String concertId,

        @NotBlank(message = "사용자 ID는 필수입니다.")
        String userId,

        @NotBlank(message = "토큰은 필수입니다.")
        String token
) {}
