package ma.luan.yiyan;

import io.vertx.core.AbstractVerticle;


import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
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
    public void start(Future<Void> startFuture) {
        // 配置 RuntimeError 错误记录
        vertx.exceptionHandler(error -> log.error(error));

        // 顺序部署 Verticle
        Future.<Void>succeededFuture()
                .compose(v -> Future.<String>future(s -> vertx.deployVerticle(new ApiVerticle(), new DeploymentOptions().setConfig(config()), s)))
                .compose(v -> Future.<String>future(s -> vertx.deployVerticle(new DataService(), new DeploymentOptions().setConfig(config()), s)))
                .compose(v -> Future.<String>future(s -> vertx.deployVerticle(new LogService(), new DeploymentOptions().setConfig(config()), s)))
                .setHandler(result -> {
                    if (result.succeeded()) {
                        startFuture.complete();
                    } else {
                        startFuture.fail("Vert.x failed to start");
                    }
                });
    }
}
