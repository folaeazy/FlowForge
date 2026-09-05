-- rate_limit.lua
-- Atomic Token Bucket rate limiting for FlowForge.
--
-- Why Lua:
-- The read → refill → check → deduct → write sequence has a race condition
-- if done in application code (even with Redis WATCH/MULTI).
-- Lua scripts execute atomically on the Redis server thread —
-- no other client can interleave between any two lines here.
--
-- Input:
--   KEYS[1] = tenant key  e.g. "flowforge:ratelimit:tenant-A"
--   ARGV[1] = capacity         (max real tokens, e.g. 100)
--   ARGV[2] = tokensPerSecond  (refill rate, e.g. 50)
--   ARGV[3] = now              (System.currentTimeMillis())
--   ARGV[4] = requested        (tokens to consume, usually 1)
--   ARGV[5] = scale            (1000 — matches Java TokenBucket)
--
-- Returns:
--   >= 0  → ALLOWED. Value is remaining scaled tokens after deduction.
--   -1    → REJECTED. Insufficient tokens.
--
-- Key TTL: 3600 seconds (1 hour of inactivity cleans up automatically)

local key            = KEYS[1]
local capacity       = tonumber(ARGV[1])
local tokensPerSecond= tonumber(ARGV[2])
local now            = tonumber(ARGV[3])
local requested      = tonumber(ARGV[4])
local scale          = tonumber(ARGV[5])

local capacityScaled = capacity * scale
local requiredScaled = requested * scale

-- Read current state
-- HMGET returns a table: {storedTokens, lastRefill}
-- Values are nil if this tenant has never been seen
local data       = redis.call('HMGET', key, 'tokens', 'last_refill')
local tokens     = tonumber(data[1])
local lastRefill = tonumber(data[2])

-- First request from this tenant: initialise to full capacity
if tokens == nil then
    tokens = capacityScaled
end
if lastRefill == nil then
    lastRefill = now
end

-- Refill calculation — same formula as Java TokenBucket:
-- tokensToAdd = (elapsedMs * tokensPerSecond * scale) / 1000
-- Multiply before divide — avoids integer truncation on small rates
local elapsedMs   = now - lastRefill
if elapsedMs < 0 then elapsedMs = 0 end  -- clock safety

local tokensToAdd = math.floor((elapsedMs * tokensPerSecond * scale) / 1000)
local currentTokens = math.min(capacityScaled, tokens + tokensToAdd)

-- Decision
if currentTokens < requiredScaled then
    -- Rejected: still update refill state so next call sees correct elapsed time
    -- Do NOT deduct — bucket stays at currentTokens
    redis.call('HMSET', key, 'tokens', currentTokens, 'last_refill', now)
    redis.call('EXPIRE', key, 3600)
    return -1
end

-- Allowed: deduct and persist
local remaining = currentTokens - requiredScaled
redis.call('HMSET', key, 'tokens', remaining, 'last_refill', now)
redis.call('EXPIRE', key, 3600)

return remaining  -- Java divides by SCALE to get real token count