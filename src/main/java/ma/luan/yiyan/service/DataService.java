package ma.luan.yiyan.service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import ma.luan.yiyan.constants.Key;
import ma.luan.yiyan.util.CategoryTrie;
import ma.luan.yiyan.util.JsonCollector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;


public class DataService extends AbstractVerticle {
    private RedisClient redisClient;
    private Random random = new Random();
    private RedisOptions redisOptions;
    private Logger log = LogManager.getLogger(this.getClass());
    private static CategoryTrie keysInRedis = new CategoryTrie();

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
                imgKeys.result().addAll(jsonKeys.result())
                    .stream()
                    .forEach(key -> keysInRedis.insert((String) key));
                startFuture.complete();
            } else {
                log.error("DataService fail to start", v.cause());
                startFuture.fail(v.cause());
            }
        });
    }

    private void getHelpFromRedis(Message message) {
        redisClient.lrange(Key.REDIS_HELP_LIST, 0, -1, res -> {
            if (res.succeeded()) {
                JsonArray array = res.result();
                JsonArray newArray = array.stream()
                    .map(text -> {
                        String prefix = config().getString("api.url", "http://localhost/");
                        return new JsonObject((String) text).stream()
                            .collect(Collectors.toMap(Map.Entry::getKey,
                                v -> prefix + v.getValue().toString().replace(":", "/")));
                    })
                    .collect(JsonCollector.toJsonArray());
                message.reply(newArray);
            } else {
                log.error("Fail to get data from Redis", res.cause());
                message.fail(500, res.cause().getMessage());
            }
        });
    }

    /**
     * @param message example: {format: "png", categories: [shenghuo, buyi]}
     */
    private void getGushiciFromRedis(Message<JsonObject> message) {
        JsonArray realCategory = new JsonArray()
            .add("png".equals(message.body().getString("format")) ? "img" : "json")
            .addAll(message.body().getJsonArray("categories"));
        checkAndGetKey(realCategory)
            .compose(key -> Future.<String>future(s -> redisClient.srandmember(key, s))) // 从 set 随机返回一个对象
            .setHandler(res -> {
                if (res.succeeded()) {
                    message.reply(res.result());
                } else {
                    if (res.cause() instanceof ReplyException) {
                        ReplyException exception = (ReplyException) res.cause();
                        message.fail(exception.failureCode(), exception.getMessage());
                    }
                    message.fail(500, res.cause().getMessage());
                }
            });
    }

    /**
     * @param categories 用户请求的类别 [img, shenghuo ,buyi]
     * @return 返回一个随机类别的 key （set)
     */
    private Future<String> checkAndGetKey(JsonArray categories) {
        Future<String> result = Future.future();
        List<String> toRandom = keysInRedis.getKeys(categories);
        if (toRandom.size() >= 1) {
            result.complete(toRandom.get(random.nextInt(toRandom.size())));
        } else {
            result.fail(new ReplyException(ReplyFailure.RECIPIENT_FAILURE, 404, "没有结果，请检查API"));
        }
        return result;
    }
}

