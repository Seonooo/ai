# language: ko
기능: 대기열 시스템
  콘서트 티켓팅을 위한 대기열 관리
  대규모 트래픽 처리를 위해 Wait Queue와 Active Queue를 운영한다

  배경:
    Given 대기열 시스템이 준비되어 있다

  시나리오: 사용자가 대기열에 진입한다
    Given 콘서트 "CONCERT-001"이 있다
    And 사용자 "USER-001"이 있다
    When 사용자가 대기열 진입을 요청한다
    Then 대기열 진입이 성공한다
    And 대기 순번을 받는다

  시나리오: 대기 중인 사용자가 상태를 조회한다
    Given 콘서트 "CONCERT-001"이 있다
    And 사용자 "USER-001"이 대기 큐에 있다
    When 사용자가 대기열 상태를 조회한다
    Then 상태가 "WAITING"이다
    And 대기 순번이 표시된다

  시나리오: 대기 중인 사용자가 Active Queue로 전환된다
    Given 콘서트 "CONCERT-001"이 있다
    And 사용자 "USER-001"이 대기 큐에 있다
    When 스케줄러가 대기열 전환을 실행한다
    Then 사용자가 Active Queue로 이동한다
    And 토큰을 받는다
    And 상태가 "READY"이다

  시나리오: Active 사용자가 예매 페이지 접속으로 토큰을 활성화한다
    Given 콘서트 "CONCERT-001"이 있다
    And 사용자 "USER-001"이 활성 큐에 있다
    When 사용자가 토큰 활성화를 요청한다
    Then 토큰 활성화가 성공한다
    And 상태가 "ACTIVE"로 변경된다
    And 만료 시간이 10분으로 연장된다

  시나리오: Active 사용자가 토큰을 연장한다
    Given 콘서트 "CONCERT-001"이 있다
    And 사용자 "USER-001"이 활성 상태이다
    When 사용자가 토큰 연장을 요청한다
    Then 토큰 연장이 성공한다
    And 연장 횟수가 증가한다
    And 만료 시간이 갱신된다

  시나리오: 사용자가 최대 연장 횟수를 초과하여 연장을 시도한다
    Given 콘서트 "CONCERT-001"이 있다
    And 사용자 "USER-001"이 활성 상태이다
    And 이미 2회 연장했다
    When 사용자가 토큰 연장을 요청한다
    Then 연장이 실패한다
    And 에러 메시지에 "더 이상 연장할 수 없습니다"가 포함된다

  시나리오: 유효한 토큰을 검증한다
    Given 콘서트 "CONCERT-001"이 있다
    And 사용자 "USER-001"이 활성 상태이다
    And 유효한 토큰 "TOKEN-123"을 가지고 있다
    When 토큰 검증을 요청한다
    Then 토큰 검증이 성공한다

  시나리오: 만료된 토큰을 검증한다
    Given 콘서트 "CONCERT-001"이 있다
    And 사용자 "USER-001"의 토큰이 만료되었다
    When 토큰 검증을 요청한다
    Then 토큰 검증이 실패한다
    And 에러 코드가 "QUEUE_TOKEN_EXPIRED"이다

  시나리오: 결제 완료 후 대기열에서 제거된다
    Given 콘서트 "CONCERT-001"이 있다
    And 사용자 "USER-001"이 활성 상태이다
    When 결제 완료 이벤트가 발행된다
    Then 사용자가 Active Queue에서 제거된다
    And 상태 조회 시 "NOT_FOUND"이다

  시나리오: 만료된 토큰이 정리된다
    Given 콘서트 "CONCERT-001"이 있다
    And 사용자 "USER-001"의 토큰이 만료되었다
    And 사용자 "USER-002"의 토큰이 만료되었다
    When 스케줄러가 만료 토큰 정리를 실행한다
    Then 만료된 토큰 2개가 제거된다

  시나리오: 동시에 여러 사용자가 대기열에 진입한다
    Given 콘서트 "CONCERT-001"이 있다
    When 100명의 사용자가 동시에 진입을 요청한다
    Then 모든 사용자가 대기열에 추가된다
    And 각 사용자는 고유한 순번을 받는다
