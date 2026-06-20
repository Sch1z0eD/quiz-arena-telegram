-- Atomic matchmaking: pair with a waiting opponent or join the queue.
-- LPOP is atomic, so the same waiting player can never be handed to two searchers.
-- KEYS[1] = queue list (mm:{cat}:{diff})
-- KEYS[2] = searcher busy key (mm:busy:{userId})
-- ARGV[1] = searcher entry "userId:chatId:msgId"
-- ARGV[2] = ttl seconds
-- Returns: "BUSY" | "QUEUED" | "MATCH:<opponentEntry>"
if redis.call('EXISTS', KEYS[2]) == 1 then
    return 'BUSY'
end
local opponent = redis.call('LPOP', KEYS[1])
if opponent then
    redis.call('SET', KEYS[2], '1', 'EX', ARGV[2])
    return 'MATCH:' .. opponent
end
redis.call('SET', KEYS[2], '1', 'EX', ARGV[2])
redis.call('RPUSH', KEYS[1], ARGV[1])
redis.call('EXPIRE', KEYS[1], ARGV[2])
return 'QUEUED'
