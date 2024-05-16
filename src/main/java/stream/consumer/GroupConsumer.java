package stream.consumer;

import io.lettuce.core.Consumer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XAutoClaimArgs;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisStreamCommands;
import io.lettuce.core.models.stream.ClaimedMessages;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
class GroupConsumer {

    public static void main(String[] args) {
        try (RedisClient redisClient = RedisClient.create("redis://127.0.0.1:6379")) {
            StatefulRedisConnection<String, String> connection = redisClient.connect();
            RedisStreamCommands<String, String> streamCommands = connection.sync();

            String streamName = "stream";
            String lastSeenOffset = "0-0";

            String consumerGroup = "Group1";
            String consumerName = "consumer1";
            while (true) {
                List<StreamMessage<String, String>> messages = new ArrayList();

                Consumer<String> from = Consumer.from(consumerGroup, consumerName);
                XReadArgs count = XReadArgs.Builder.count(3);
                XReadArgs.StreamOffset<String> offset = XReadArgs.StreamOffset.lastConsumed(streamName);
                List<StreamMessage<String, String>> newMessages = streamCommands.xreadgroup(from, count, offset);

                XAutoClaimArgs<String> autoClaimArgs = XAutoClaimArgs.Builder.xautoclaim(from, 60000, "0-0").count(5);
                ClaimedMessages<String, String> claimedMessages = streamCommands.xautoclaim(streamName, autoClaimArgs);

                messages.addAll(newMessages);
                messages.addAll(claimedMessages.getMessages());

                if (!messages.isEmpty()) {
                    for (StreamMessage<String, String> message : messages) {
                        System.out.print(lastSeenOffset + " received message");
                        System.out.print(" userId: " + message.getBody().get("userId"));
                        System.out.print(" order: " + message.getBody().get("order"));
                        System.out.println(" email: " + message.getBody().get("e-mail"));
                        streamCommands.xack(streamName, consumerGroup, message.getId());
                    }
                }
            }
        }
    }
}
