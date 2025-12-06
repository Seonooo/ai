package personal.ai.core.payment.adapter.in.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import personal.ai.common.dto.ApiResponse;
import personal.ai.core.payment.adapter.in.web.dto.PaymentResponse;
import personal.ai.core.payment.adapter.in.web.dto.ProcessPaymentRequest;
import personal.ai.core.payment.application.port.in.ProcessPaymentUseCase;
import personal.ai.core.payment.domain.model.Payment;

/**
 * Payment REST Controller
 * 결제 API 엔드포인트
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;

    /**
     * 결제 처리
     * POST /api/v1/payments
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @RequestBody ProcessPaymentRequest request) {

        log.info("Processing payment request: reservationId={}, userId={}",
                request.reservationId(), request.userId());

        // Use Case 실행
        ProcessPaymentUseCase.ProcessPaymentCommand command =
                new ProcessPaymentUseCase.ProcessPaymentCommand(
                        request.reservationId(),
                        request.userId(),
                        request.amount(),
                        request.paymentMethod(),
                        request.concertId()
                );

        Payment payment = processPaymentUseCase.processPayment(command);

        // 응답 생성
        PaymentResponse response = PaymentResponse.from(payment);

        log.info("Payment processed successfully: paymentId={}", payment.id());

        // ApiResponse 포맷으로 감싸서 반환
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("결제가 완료되었습니다.", response));
    }
}
