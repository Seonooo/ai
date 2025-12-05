-- activate_token.lua
-- READY → ACTIVE 상태 전환 및 만료 시간 갱신을 원자적으로 처리
--
-- KEYS[1]: Active Queue Key (ZSet)
-- KEYS[2]: Token Key (Hash)
-- ARGV[1]: User ID
-- ARGV[2]: New Expiration Time (epoch seconds, ACTIVE 상태 만료 시간)
-- ARGV[3]: TTL (seconds)
--
-- Return:
--   1: 성공 (READY → ACTIVE 전환)
--   0: 실패 (토큰 없음 또는 이미 ACTIVE)
--   -1: 이미 ACTIVE 상태

local queueKey = KEYS[1]
local tokenKey = KEYS[2]
local userId = ARGV[1]
local newExpiredAt = tonumber(ARGV[2])
local ttl = tonumber(ARGV[3])

-- 1. Hash가 존재하는지 확인
if redis.call('EXISTS', tokenKey) == 0 then
    return 0  -- 토큰 없음
end

-- 2. 현재 상태 확인
local currentStatus = redis.call('HGET', tokenKey, 'status')

if currentStatus == 'ACTIVE' then
    return -1  -- 이미 ACTIVE 상태
end

if currentStatus ~= 'READY' then
    return 0  -- READY 상태가 아니면 활성화 불가
end

-- 3. READY → ACTIVE 전환 (원자적)
-- 3-1. Hash 상태 변경
redis.call('HSET', tokenKey, 'status', 'ACTIVE')

-- 3-2. Hash 만료 시간 갱신
redis.call('HSET', tokenKey, 'expired_at', newExpiredAt)

-- 3-3. ZSet Score 갱신
redis.call('ZADD', queueKey, newExpiredAt, userId)

-- 3-4. Hash TTL 갱신
if ttl > 0 then
    redis.call('EXPIRE', tokenKey, ttl)
end

return 1  -- 성공
