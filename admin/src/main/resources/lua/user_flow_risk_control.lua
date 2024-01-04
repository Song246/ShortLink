-- redis 内置支持lua 脚本；访问次数INCR指令没有过期时间设置设置，所以使用lua 脚本保证原子性
-- 设置用户访问频率限制的参数（一个键参数 KEYS[1]，一个值参数ARGV[1]）
local username = KEYS[1]
local timeWindow = tonumber(ARGV[1]) -- 时间窗口，单位：秒

-- 构造 Redis 中存储用户访问次数的键名
local accessKey = "short-link:user-flow-risk-control:" .. username

-- 原子递增访问次数，并获取递增后的值
local currentAccessCount = redis.call("INCR", accessKey)

-- 设置键的过期时间
redis.call("EXPIRE", accessKey, timeWindow)

-- 返回当前访问次数,根据访问次数去判断是否恶意流量请求 进行流量风控
return currentAccessCount