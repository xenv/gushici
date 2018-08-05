package ma.luan.yiyan.service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import ma.luan.yiyan.constants.Key;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;


public class DataService extends AbstractVerticle {
    private RedisClient redisClient;
    private Random random = new Random();
    private RedisOptions redisOptions;
    private Logger log = LogManager.getLogger(this.getClass());
    private static List<String> keysInRedis;

    public DataService(RedisOptions redisOptions) {
        this.redisOptions = redisOptions;
    }

    @Override
    public void start(Future<Void> startFuture) {
        vertx.eventBus().consumer(Key.GET_GUSHICI_FROM_REDIS, this::getGushiciFromRedis);
        vertx.eventBus().consumer(Key.GET_HELP_FROM_REDIS, this::getHelpFromRedis);
        redisClient = RedisClient.create(vertx, redisOptions);
        // 从 redis 缓存所有 key
        Future<JsonArray> imgKeys = Future.future(f -> redisClient.keys(Key.IMG, f));
        Future<JsonArray> jsonKeys = Future.future(f -> redisClient.keys(Key.JSON, f));
        CompositeFuture.all(Arrays.asList(imgKeys, jsonKeys)).setHandler(v -> {
            if (v.succeeded()) {
                keysInRedis = imgKeys.result().addAll(jsonKeys.result()).stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.toList());
                startFuture.complete();
            } else {
                startFuture.fail(v.cause());
            }
        });
    }

    private void getHelpFromRedis(Message message) {
        redisClient.lrange(Key.REDIS_HELP_LIST, 0, -1, res -> {
            if (res.succeeded()) {
                JsonArray array = res.result();
                JsonArray newArray = new JsonArray(array.stream()
                    .map(text -> {
                        // 无力吐槽 JsonArray 的 stream 操作
                        JsonObject result = new JsonObject();
                        String prefix = config().getString("index.url", "http://localhost/");
                        new JsonObject((String) text).stream()
                            .forEach(entry -> result.put(entry.getKey(), prefix +
                                entry.getValue().toString().replace(":", "/")));
                        return result;
                    })
                    .collect(Collectors.toList())
                );
                message.reply(newArray);
            } else {
                message.reply(res.cause());
            }
        });
    }

    private void getGushiciFromRedis(Message<JsonObject> message) {
        String keyName = message.body().getJsonArray("classes").stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .collect(Collectors.joining(":"));
        keyName = ("png".equals(message.body().getString("format")) ? "img" : "json") + ":" + keyName;

        checkAndGetKey(keyName)
            .compose(key -> Future.<String>future(s -> redisClient.srandmember(key, s))) // 从 set 随机返回一个对象
            .setHandler(res -> {
                if (res.succeeded()) {
                    message.reply(res.result());
                } else {
                    message.fail(404, res.cause().getMessage());
                }
            });
    }

    /**
     * @param keys 用户请求的类别
     * @return 返回一个随机类别的 key （set)
     */
    private Future<String> checkAndGetKey(String keys) {
        Future<String> result = Future.future();
        // 这里可以改用多级 Map 减少随机选择范围，不过创建 Map 也要一些开销
        List<String> toRandom = keysInRedis.stream()
            .filter(key -> key.startsWith(keys))
            .collect(Collectors.toList());
        if (toRandom.size() >= 1) {
            result.complete(toRandom.get(random.nextInt(toRandom.size())));
        } else {
            result.fail("404, 没有结果，请检查API");
        }
        return result;

    }
}

