package distributedlock;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

class DistributedLock {

    // Lock expiry time in milliseconds
    private static final int LOCK_EXPIRY = 10000;

    // Time to wait between retry attempts
    private static final int RETRY_WAIT_TIME = 100;

    public static void main(String[] args) {
        try (RedisClient redisClient = RedisClient.create("redis://127.0.0.1:6379")) {

            String lockKey = "distributed_lock";
            if (acquireLock(redisClient, lockKey)) {

                releaseLock(redisClient, lockKey);
            }
        }
    }

    private static boolean acquireLock(RedisClient redisClient, String lockKey) {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            // Try to acquire the lock
            long startTime = System.currentTimeMillis();
            while ((System.currentTimeMillis() - startTime) < LOCK_EXPIRY) {
                String result = commands.set(lockKey, "locked");
                if ("OK".equals(result)) {
                    // Lock acquired successfully
                    return true;
                }
                // Wait before retrying
                try {
                    Thread.sleep(RETRY_WAIT_TIME);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        // Lock acquisition failed
        return false;
    }

    private static void releaseLock(RedisClient redisClient, String lockKey) {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            commands.del(lockKey);
        }
    }


}
