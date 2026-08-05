package com.exam.queue.repository;

import com.exam.queue.domain.QueueTokenInfo;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class RedisQueueRepository {

    private static final DefaultRedisScript<Long> DELETE_IF_MATCHES =
            new DefaultRedisScript<>(
                    """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                        return redis.call('del', KEYS[1])
                    else
                        return 0
                    end
                    """,
                    Long.class
            );

    private final StringRedisTemplate redisTemplate;

    public RedisQueueRepository(
            StringRedisTemplate redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    public void saveToken(
            String token,
            Long scheduleId,
            String userId,
            Duration ttl
    ) {
        String key = tokenKey(token);

        redisTemplate.opsForHash().putAll(
                key,
                Map.of(
                        "scheduleId", scheduleId.toString(),
                        "userId", userId
                )
        );

        redisTemplate.expire(key, ttl);
    }

    public Optional<QueueTokenInfo> findToken(String token) {
        String key = tokenKey(token);

        Object scheduleId =
                redisTemplate.opsForHash().get(key, "scheduleId");

        Object userId =
                redisTemplate.opsForHash().get(key, "userId");

        if (scheduleId == null || userId == null) {
            return Optional.empty();
        }

        return Optional.of(
                new QueueTokenInfo(
                        Long.valueOf(scheduleId.toString()),
                        userId.toString()
                )
        );
    }

    public boolean claimUserToken(
            Long scheduleId,
            String userId,
            String token,
            Duration ttl
    ) {
        Boolean result =
                redisTemplate.opsForValue().setIfAbsent(
                        userTokenKey(scheduleId, userId),
                        token,
                        ttl
                );

        return Boolean.TRUE.equals(result);
    }

    public String getUserToken(
            Long scheduleId,
            String userId
    ) {
        return redisTemplate.opsForValue().get(
                userTokenKey(scheduleId, userId)
        );
    }

    public void addWaiting(
            Long scheduleId,
            String token,
            long registeredAt
    ) {
        redisTemplate.opsForZSet().add(
                waitingKey(scheduleId),
                token,
                registeredAt
        );
    }

    public Long getWaitingRank(
            Long scheduleId,
            String token
    ) {
        return redisTemplate.opsForZSet().rank(
                waitingKey(scheduleId),
                token
        );
    }

    public Set<String> getFirstWaiting(
            Long scheduleId,
            long count
    ) {
        if (count <= 0) {
            return Set.of();
        }

        return redisTemplate.opsForZSet().range(
                waitingKey(scheduleId),
                0,
                count - 1
        );
    }

    public void removeWaiting(
            Long scheduleId,
            String token
    ) {
        redisTemplate.opsForZSet().remove(
                waitingKey(scheduleId),
                token
        );
    }

    public boolean moveToActive(
            Long scheduleId,
            String token,
            long expiresAt
    ) {
        Long removed = redisTemplate.opsForZSet().remove(
                waitingKey(scheduleId),
                token
        );

        if (removed == null || removed == 0) {
            return false;
        }

        redisTemplate.opsForZSet().add(
                activeKey(scheduleId),
                token,
                expiresAt
        );

        return true;
    }

    public void removeExpiredActive(
            Long scheduleId,
            long currentTime
    ) {
        redisTemplate.opsForZSet().removeRangeByScore(
                activeKey(scheduleId),
                0,
                currentTime
        );
    }

    public long getActiveCount(Long scheduleId) {
        Long count = redisTemplate.opsForZSet().size(
                activeKey(scheduleId)
        );

        return count == null ? 0 : count;
    }

    public boolean isActive(
            Long scheduleId,
            String token
    ) {
        Double score = redisTemplate.opsForZSet().score(
                activeKey(scheduleId),
                token
        );

        return score != null;
    }

    public void removeActive(
            Long scheduleId,
            String token
    ) {
        redisTemplate.opsForZSet().remove(
                activeKey(scheduleId),
                token
        );
    }

    public void refreshToken(
            String token,
            Duration ttl
    ) {
        redisTemplate.expire(tokenKey(token), ttl);
    }

    public void refreshUserToken(
            Long scheduleId,
            String userId,
            Duration ttl
    ) {
        redisTemplate.expire(
                userTokenKey(scheduleId, userId),
                ttl
        );
    }

    public boolean acquireAdmissionLock(
            Long scheduleId,
            String lockOwner,
            Duration ttl
    ) {
        Boolean result =
                redisTemplate.opsForValue().setIfAbsent(
                        lockKey(scheduleId),
                        lockOwner,
                        ttl
                );

        return Boolean.TRUE.equals(result);
    }

    public void releaseAdmissionLock(
            Long scheduleId,
            String lockOwner
    ) {
        redisTemplate.execute(
                DELETE_IF_MATCHES,
                Collections.singletonList(lockKey(scheduleId)),
                lockOwner
        );
    }

    public void deleteToken(String token) {
        redisTemplate.delete(tokenKey(token));
    }

    public void deleteUserToken(
            Long scheduleId,
            String userId,
            String token
    ) {
        redisTemplate.execute(
                DELETE_IF_MATCHES,
                Collections.singletonList(
                        userTokenKey(scheduleId, userId)
                ),
                token
        );
    }

    private String waitingKey(Long scheduleId) {
        return "queue:{" + scheduleId + "}:waiting";
    }

    private String activeKey(Long scheduleId) {
        return "queue:{" + scheduleId + "}:active";
    }

    private String tokenKey(String token) {
        return "queue:token:" + token;
    }

    private String userTokenKey(
            Long scheduleId,
            String userId
    ) {
        return "queue:user:" + scheduleId + ":" + userId;
    }

    private String lockKey(Long scheduleId) {
        return "queue:{" + scheduleId + "}:admission-lock";
    }
}