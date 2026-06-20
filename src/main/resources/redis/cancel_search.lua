-- Atomic cancel/timeout: remove the player from the queue and clear busy.
-- Returns 1 if the player was still waiting (cancelled), 0 if already matched.
-- KEYS[1] = queue list (mm:{cat}:{diff})
-- KEYS[2] = busy key (mm:busy:{userId})
-- ARGV[1] = entry "userId:chatId:msgId"
local removed = redis.call('LREM', KEYS[1], 0, ARGV[1])
if removed > 0 then
    redis.call('DEL', KEYS[2])
    return 1
end
return 0
