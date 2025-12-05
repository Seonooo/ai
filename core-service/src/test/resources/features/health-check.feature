Feature: Core Service Health Check
  Core 서비스의 헬스 체크 기능을 검증한다

  Scenario: 헬스 체크 API 호출
    Given Core 서비스가 정상 동작 중이다
    When 헬스 체크 API를 호출하면
    Then 응답 상태 코드는 200이다
    And 응답 결과는 "success"이다
    And 데이터베이스 상태 정보가 포함되어 있다
    And Redis 상태 정보가 포함되어 있다
    And Kafka 상태 정보가 포함되어 있다
