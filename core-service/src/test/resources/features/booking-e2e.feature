Feature: 콘서트 예매 E2E 플로우
  사용자가 대기열 진입부터 예약 완료까지 전체 프로세스를 완료한다

  @e2e @booking-flow
  Scenario: 정상 예매 플로우 - 대기열 진입 > 좌석 선점 > 결제 > 예약 완료
    # 테스트 데이터 준비
    Given 스케줄 ID 1번에 좌석 "A5"가 "AVAILABLE" 상태로 존재한다
    And 사용자 "testuser"가 등록되어 있다

    # Step 1: 대기열 진입
    When 사용자가 대기열 활성 토큰을 발급받는다
    Then 활성 토큰이 발급된다

    # Step 2: 좌석 선점 (예약 생성)
    When 사용자가 좌석을 예약한다
    Then 예약이 생성되고 상태는 "PENDING"이다
    And 좌석 상태는 "RESERVED"로 변경된다

    # Step 3: 결제 진행
    When 사용자가 결제를 진행한다
    Then 결제가 완료되고 상태는 "COMPLETED"이다

    # Step 4: Outbox 이벤트 확인
    Then 결제 완료 이벤트가 Outbox 테이블에 "PENDING" 상태로 저장된다

    # Step 5: 이벤트 발행 및 예약 확정 (비동기)
    When Outbox 스케줄러가 실행된다
    Then Outbox 이벤트 상태가 "PUBLISHED"로 변경된다
