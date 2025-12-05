-- add_to_active_queue.lua
-- KEYS[1]: Active Queue Key (ZSet)
-- KEYS[2]: Token Key (Hash)
-- ARGV[1]: UserId
-- ARGV[2]: Score (ExpiredAt Epoch Second)
-- ARGV[3]: Token Value
-- ARGV[4]: Status (READY)
-- ARGV[5]: Extend Count (0)
-- ARGV[6]: ExpiredAt String
-- ARGV[7]: TTL Seconds

-- 1. Add to Active Queue (ZSet)
redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])

-- 2. Set Token Data (Hash)
redis.call('HMSET', KEYS[2], 
    'token', ARGV[3],
    'status', ARGV[4],
    'extend_count', ARGV[5],
    'expired_at', ARGV[6]
)

-- 3. Set TTL for Hash
redis.call('EXPIRE', KEYS[2], ARGV[7])

return 1
