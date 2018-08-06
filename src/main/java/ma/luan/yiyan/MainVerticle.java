package ma.luan.yiyan;

import io.vertx.core.AbstractVerticle;


import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.redis.RedisOptions;
import ma.luan.yiyan.api.ApiVerticle;
import ma.luan.yiyan.service.DataService;
import ma.luan.yiyan.service.LogService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * @author 乱码 https://luan.ma/
 */
public class MainVerticle extends AbstractVerticle {

    private Logger log = LogManager.getLogger(this.getClass());

    @Override
    public void start() {

        // 读取配置文件
        RedisOptions redisOptions = new RedisOptions()
            .setHost(config().getJsonObject("redis").getString("host","127.0.0.1"))
            .setPort(config().getJsonObject("redis").getInteger("port",6379))
            .setSelect(config().getJsonObject("redis").getInteger("select",0));

        // 配置 RuntimeError 错误记录
        vertx.exceptionHandler(error -> log.error(error));

        // 顺序部署 Verticle
        Future.<Void>succeededFuture()
            .compose(v -> Future.<String>future(s -> vertx.deployVerticle(new ApiVerticle(),new DeploymentOptions().setConfig(config()), s)))
            .compose(v -> Future.<String>future(s -> vertx.deployVerticle(new DataService(redisOptions), new DeploymentOptions().setConfig(config()),s)))
            .compose(v -> Future.<String>future(s -> vertx.deployVerticle(new LogService(redisOptions), s)))
            .compose(v -> log.info("Vert.x started successfully"),
                Future.future().setHandler(ar -> {
                    if (ar.failed()) {
                        log.error("Vert.x failed to start", ar.cause());
                    }
                }));
    }
}
