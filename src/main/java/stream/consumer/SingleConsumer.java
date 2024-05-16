package stream.consumer;

import io.lettuce.core.RedisClient;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisStreamCommands;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
class SingleConsumer {

    public static void main(String[] args) {
        try (RedisClient redisClient = RedisClient.create("redis://127.0.0.1:6379")) {
            StatefulRedisConnection<String, String> connection = redisClient.connect();
            RedisStreamCommands<String, String> streamCommands = connection.sync();

            String streamName = "stream";
            String lastSeenOffset = "0-0";

            while (true) {
                XReadArgs count = XReadArgs.Builder.block(500).count(1);
                XReadArgs.StreamOffset<String> from = XReadArgs.StreamOffset.from(streamName, lastSeenOffset);

                List<StreamMessage<String, String>> streamMessages = streamCommands.xread(count, from);
                for (StreamMessage<String, String> message : streamMessages) {
                    lastSeenOffset = message.getId();


                    Map<String, String> body = message.getBody();

                    System.out.print(lastSeenOffset + " received message");
                    System.out.print(" userId: " + body.get("userId"));
                    System.out.print(" order: " + body.get("order"));
                    System.out.println(" email: " + body.get("e-mail"));
                }
            }
        }
    }
}
