package ma.luan.yiyan.service;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import ma.luan.yiyan.constants.Key;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;


@RunWith(VertxUnitRunner.class)
public class LogServiceTest {
    private Vertx vertx;
    private JsonObject config;

    @Before
    public void beforeClass(TestContext context) {
        Async async = context.async();
        vertx = Vertx.vertx();
        config = new JsonObject(vertx.
                fileSystem().readFileBlocking("conf.json"));
        vertx.deployVerticle(new LogService(), new DeploymentOptions().setConfig(config), c -> {
            async.complete();
        });
    }

    @Test
    public void testLogService(TestContext context) {
        Async async = context.async();
        vertx.eventBus().send(Key.GET_HISTORY_FROM_REDIS, null, r1 -> {
            if (r1.succeeded()) {
                JsonObject o = (JsonObject) r1.result().body();
                context.verify(c -> assertThat(Integer.parseInt(o.getString("总点击量"))
                        , greaterThan(-1)));
                vertx.eventBus().publish(Key.SET_HISTORY_TO_REDIS, null);
                // 历史记录  + 1
                vertx.eventBus().send(Key.GET_HISTORY_FROM_REDIS, null, r3 -> {
                    if (r3.succeeded()) {
                        JsonObject n = (JsonObject) r3.result().body();
                        context.assertEquals(1,
                                Integer.parseInt(n.getString("总点击量"))
                                        - Integer.parseInt(o.getString("总点击量")));
                        context.assertEquals(1,
                                Integer.parseInt(n.getJsonArray("最近七天点击量").getString(0))
                                        - Integer.parseInt(o.getJsonArray("最近七天点击量").getString(0)));
                        async.complete();
                    } else {
                        context.fail();
                    }
                });
            } else {
                context.fail();
            }
        });
    }
}
