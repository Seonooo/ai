# language: ko
@validation
기능: 예약/결제 API Validation 검증
  잘못된 입력값에 대해 적절한 에러를 반환해야 한다

  배경:
    Given 예약 가능한 콘서트 스케줄이 존재한다

  # ==========================================
  # 좌석 조회 Validation
  # ==========================================

  @seats @validation
  시나리오: scheduleId 없이 좌석 조회를 시도한다
    When scheduleId 없이 좌석 조회를 요청한다
    Then 요청이 거부된다
    And 잘못된 입력값이라는 메시지가 반환된다

  @seats @validation
  시나리오: 대기열 토큰 없이 좌석 조회를 시도한다
    When 대기열 토큰 없이 좌석 조회를 요청한다
    Then 인증이 거부된다
    And 대기열 토큰이 필요하다는 메시지가 반환된다

  @seats @validation
  시나리오: 잘못된 대기열 토큰으로 좌석 조회를 시도한다
    When 잘못된 대기열 토큰으로 좌석 조회를 요청한다
    Then 인증이 거부된다
    And 유효하지 않은 토큰이라는 메시지가 반환된다

  # ==========================================
  # 예약 생성 Validation
  # ==========================================

  @reservation @validation
  시나리오: scheduleId 없이 예약을 요청한다
    Given 예약 가능한 좌석이 있다
    When scheduleId 없이 예약을 요청한다
    Then 요청이 거부된다
    And 잘못된 입력값이라는 메시지가 반환된다

  @reservation @validation
  시나리오: seatId 없이 예약을 요청한다
    Given 예약 가능한 좌석이 있다
    When seatId 없이 예약을 요청한다
    Then 요청이 거부된다
    And 잘못된 입력값이라는 메시지가 반환된다

  @reservation @validation
  시나리오: userId 헤더 없이 예약을 요청한다
    Given 예약 가능한 좌석이 있다
    When userId 헤더 없이 예약을 요청한다
    Then 인증이 거부된다
    And 사용자 인증이 필요하다는 메시지가 반환된다

  @reservation @validation
  시나리오: 대기열 토큰 없이 예약을 요청한다
    Given 예약 가능한 좌석이 있다
    When 대기열 토큰 없이 예약을 요청한다
    Then 인증이 거부된다
    And 대기열 토큰이 필요하다는 메시지가 반환된다

  # ==========================================
  # 결제 Validation
  # ==========================================

  @payment @validation
  시나리오: reservationId 없이 결제를 요청한다
    When reservationId 없이 결제를 요청한다
    Then 요청이 거부된다
    And 잘못된 입력값이라는 메시지가 반환된다

  @payment @validation
  시나리오: amount 없이 결제를 요청한다
    Given 대기 중인 예약이 있다
    When amount 없이 결제를 요청한다
    Then 요청이 거부된다
    And 잘못된 입력값이라는 메시지가 반환된다

  @payment @validation
  시나리오: 음수 금액으로 결제를 요청한다
    Given 대기 중인 예약이 있다
    When 음수 금액으로 결제를 요청한다
    Then 요청이 거부된다
    And 잘못된 금액이라는 메시지가 반환된다

  @payment @validation
  시나리오: paymentMethod 없이 결제를 요청한다
    Given 대기 중인 예약이 있다
    When paymentMethod 없이 결제를 요청한다
    Then 요청이 거부된다
    And 잘못된 입력값이라는 메시지가 반환된다

  @payment @validation
  시나리오: userId 헤더 없이 결제를 요청한다
    Given 대기 중인 예약이 있다
    When userId 헤더 없이 결제를 요청한다
    Then 인증이 거부된다
    And 사용자 인증이 필요하다는 메시지가 반환된다
