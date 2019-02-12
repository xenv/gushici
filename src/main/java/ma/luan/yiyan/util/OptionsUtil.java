package ma.luan.yiyan.util;

import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisOptions;

public class OptionsUtil {
    public static RedisOptions getRedisOptions(JsonObject config) {
        return new RedisOptions()
                .setHost(config.getJsonObject("redis").getString("host","127.0.0.1"))
                .setPort(config.getJsonObject("redis").getInteger("port",6379))
                .setSelect(config.getJsonObject("redis").getInteger("select",0));
    }
}
