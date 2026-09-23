package cn.org.alan.exam.common.cache;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 业务缓存读写封装（Cache Aside）
 */
@Component
public class CacheService {

    @Resource
    private RedisTemplate redisTemplateInit;

    /**
     * 读取缓存
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplateInit.opsForValue().get(key);
    }

    /**
     * 写入缓存（带 TTL）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplateInit.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 删除单个 key
     */
    public void delete(String key) {
        redisTemplateInit.delete(key);
    }

    /**
     * 按前缀删除（用于分类树等关联 key）
     */
    @SuppressWarnings("unchecked")
    public void deleteByPrefix(String prefix) {
        Set<String> keys = redisTemplateInit.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplateInit.delete(keys);
        }
    }
}
