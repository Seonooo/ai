-- rate_limit_check.lua
-- Token Bucket 알고리즘 기반 Rate Limiting (원자성 보장)
--
-- 동작 방식:
--   - 버킷에 최대 N개의 토큰 저장 가능 (Capacity)
--   - 일정 속도(Refill Rate)로 토큰을 지속적으로 리필
--   - 요청마다 토큰 1개 소비, 토큰 부족 시 거부
--   - 마지막 리필 시간을 기준으로 경과 시간만큼 토큰 추가
--
-- 장점:
--   - Burst 트래픽 방지 (지속적인 리필로 균등한 처리)
--   - Polling 같은 지속적 요청에 적합
--   - 대규모 트래픽에서 안정적
--
-- KEYS[1]: Rate Limit Key (예: "rate_limit:queue:CONCERT-001:USER-001")
-- ARGV[1]: Capacity (버킷 최대 용량, 예: 10)
-- ARGV[2]: Refill Rate (초당 리필 토큰 수, 예: 5 = 5 tokens/sec)
-- ARGV[3]: Current Time (현재 시간, epoch seconds with milliseconds)
--
-- Redis Storage:
--   tokens: 현재 토큰 개수
--   last_refill: 마지막 리필 시간 (epoch seconds with milliseconds)
--
-- Return:
--   1: 요청 허용 (토큰 소비 성공)
--   0: 요청 거부 (토큰 부족)
--
-- 예시: Capacity=10, Refill=5/sec
--   00.0초 - 1st: tokens=10, 소비 → 9 (허용)
--   00.0초 - 2nd: tokens=9, 소비 → 8 (허용)
--   00.2초 - 3rd: 1토큰 리필(0.2*5) → 9, 소비 → 8 (허용)
--   ...10번 요청 후...
--   00.5초 - 11th: tokens=0이지만 2.5토큰 리필(0.5*5) → 2, 소비 → 1 (허용)
--   00.5초 - 12th: tokens=1, 소비 → 0 (허용)
--   00.5초 - 13th: tokens=0, 리필 없음 → 0 (거부)

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local tokensKey = key .. ':tokens'
local lastRefillKey = key .. ':last_refill'

-- 1. 현재 토큰 개수와 마지막 리필 시간 조회
local tokens = redis.call('GET', tokensKey)
local lastRefill = redis.call('GET', lastRefillKey)

-- 2. 첫 요청: 초기화
if not tokens or not lastRefill then
    -- 초기 토큰: capacity - 1 (현재 요청이 1개 소비)
    redis.call('SET', tokensKey, capacity - 1)
    redis.call('SET', lastRefillKey, now)
    -- TTL 설정: 토큰이 최대로 리필되는 시간 + 여유
    local ttl = math.ceil(capacity / refillRate) + 60
    redis.call('EXPIRE', tokensKey, ttl)
    redis.call('EXPIRE', lastRefillKey, ttl)
    return 1  -- 첫 요청 허용
end

tokens = tonumber(tokens)
lastRefill = tonumber(lastRefill)

-- 3. 경과 시간 계산 및 토큰 리필
local elapsed = now - lastRefill
if elapsed > 0 then
    local refillTokens = elapsed * refillRate
    tokens = math.min(capacity, tokens + refillTokens)

    -- 마지막 리필 시간 업데이트
    redis.call('SET', lastRefillKey, now)

    -- TTL 갱신
    local ttl = math.ceil(capacity / refillRate) + 60
    redis.call('EXPIRE', tokensKey, ttl)
    redis.call('EXPIRE', lastRefillKey, ttl)
end

-- 4. 토큰이 있으면 소비
if tokens >= 1 then
    redis.call('SET', tokensKey, tokens - 1)
    return 1  -- 요청 허용
end

-- 5. 토큰 부족: 요청 거부
return 0
