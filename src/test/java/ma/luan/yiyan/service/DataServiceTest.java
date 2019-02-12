package ma.luan.yiyan.service;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import ma.luan.yiyan.constants.Key;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


@RunWith(VertxUnitRunner.class)
public class DataServiceTest {
    private static Vertx vertx;

    @BeforeClass
    public static void beforeClass(TestContext context) {
        Async async = context.async();
        vertx = Vertx.vertx();
        JsonObject config = new JsonObject(vertx.
                fileSystem().readFileBlocking("conf.json"));
        vertx.deployVerticle(new DataService(), new DeploymentOptions().setConfig(config), c -> {
            async.complete();
        });
    }

    @Test
    public void testGetHelpFromRedis(TestContext context) {
        Async async = context.async();
        vertx.eventBus().send(Key.GET_HELP_FROM_REDIS, null, r1 -> {
            if (r1.succeeded()) {
                JsonArray array = (JsonArray) r1.result().body();
                assertThat(array.getJsonObject(0)
                        , equalTo(new JsonObject().put("全部", "https://api.gushi.ci/all")));
                async.complete();
            } else {
                context.fail();
            }
        });
    }

    @Test
    public void testGetGushiciJson1(TestContext context) {
        testGetGushiciJson(context, new JsonArray().add("dongwu").add("xiema"),
                "动物-写马");
    }

    @Test
    public void testGetGushiciJson2(TestContext context) {
        testGetGushiciJson(context, new JsonArray().add("dongwu"), "动物");
    }

    @Test
    public void testGetGushiciJson3(TestContext context) {
        testGetGushiciJson(context, new JsonArray().add("jieri"), "节日");
    }

    @Test
    public void testGetGushiciJson4(TestContext context) {
        testGetGushiciJson(context, new JsonArray(), "古诗文");
    }

    private void testGetGushiciJson(TestContext context, JsonArray test, String exceptedCategory) {
        Async async = context.async();
        JsonObject object = new JsonObject().put("format", "json").put("categories", test);
        vertx.eventBus().send(Key.GET_GUSHICI_FROM_REDIS, object, r1 -> {
            if (r1.succeeded()) {
                JsonObject result = new JsonObject((String) r1.result().body());
                context.verify(v -> assertThat(result.getString("category")
                        , containsString(exceptedCategory)));
                async.complete();
            } else {
                context.fail();
            }
        });
    }

    @Test
    public void testGetGushiciImg(TestContext context) {
        Async async = context.async();
        JsonObject object = new JsonObject().put("format", "png")
                .put("categories", new JsonArray());
        vertx.eventBus().send(Key.GET_GUSHICI_FROM_REDIS, object, r1 -> {
            if (r1.succeeded()) {
                context.verify(v -> assertThat((String) r1.result().body()
                        , startsWith("iV")));
                async.complete();
            } else {
                context.fail();
            }
        });
    }
}
