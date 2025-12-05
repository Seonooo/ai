-- remove_from_active_queue.lua
-- Active Queue에서 유저를 원자적으로 제거
-- ZSet과 Hash를 동시에 삭제
--
-- KEYS[1]: Active Queue Key (ZSet)
-- KEYS[2]: Token Key (Hash)
-- ARGV[1]: User ID
--
-- Return:
--   1: 성공 (하나 이상 제거됨)
--   0: 실패 (둘 다 존재하지 않음)

local queueKey = KEYS[1]
local tokenKey = KEYS[2]
local userId = ARGV[1]

-- 1. ZSet에서 제거
local zsetRemoved = redis.call('ZREM', queueKey, userId)

-- 2. Hash 제거
local hashRemoved = redis.call('DEL', tokenKey)

-- 하나라도 제거되었으면 성공
if zsetRemoved > 0 or hashRemoved > 0 then
    return 1
end

return 0
