-- Атомарно фиксирует ответ игрока: ответ засчитывается, только пока раунд активен.
-- KEYS[1] = ключ раунда (game:{chat}:round)
-- KEYS[2] = множество ответивших (game:{chat}:answered:{qIndex})
-- ARGV[1] = ожидаемый токен раунда
-- ARGV[2] = userId
-- ARGV[3] = TTL множества ответивших, сек
-- Возврат: -1 раунд уже завершён/сменился, 0 уже отвечал, 1 ответ принят.
if redis.call('GET', KEYS[1]) ~= ARGV[1] then
    return -1
end
local added = redis.call('SADD', KEYS[2], ARGV[2])
if added == 1 then
    redis.call('EXPIRE', KEYS[2], ARGV[3])
end
return added
