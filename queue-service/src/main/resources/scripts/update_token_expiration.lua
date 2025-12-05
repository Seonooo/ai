-- update_token_expiration.lua
-- Active Queue의 토큰 만료 시간을 원자적으로 갱신
-- ZSet Score, Hash FIELD_EXPIRED_AT, Hash TTL을 동시에 업데이트
--
-- KEYS[1]: Active Queue Key (ZSet)
-- KEYS[2]: Token Key (Hash)
-- ARGV[1]: User ID
-- ARGV[2]: New Expiration Time (epoch seconds)
-- ARGV[3]: TTL (seconds)
--
-- Return:
--   1: 성공
--   0: 실패 (토큰이 존재하지 않음)

local queueKey = KEYS[1]
local tokenKey = KEYS[2]
local userId = ARGV[1]
local expiredAt = tonumber(ARGV[2])
local ttl = tonumber(ARGV[3])

-- 1. Hash가 존재하는지 확인
if redis.call('EXISTS', tokenKey) == 0 then
    return 0  -- 토큰이 없으면 실패
end

-- 2. ZSet Score 갱신
redis.call('ZADD', queueKey, expiredAt, userId)

-- 3. Hash FIELD_EXPIRED_AT 갱신
redis.call('HSET', tokenKey, 'expired_at', expiredAt)

-- 4. Hash TTL 갱신
if ttl > 0 then
    redis.call('EXPIRE', tokenKey, ttl)
end

return 1
