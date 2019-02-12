package ma.luan.yiyan.api;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import ma.luan.yiyan.MainVerticle;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


@RunWith(VertxUnitRunner.class)
public class ApiVerticleTest {
    private WebClient webClient;

    @Before
    public void beforeClass(TestContext context) {
        Async async = context.async();
        Vertx vertx = Vertx.vertx();
        JsonObject config = new JsonObject(vertx.
                fileSystem().readFileBlocking("conf.json"));
        WebClientOptions webClientOptions = new WebClientOptions()
                .setDefaultHost(config.getString("test.host"))
                .setDefaultPort(config.getInteger("http.port"));
        webClient = WebClient.create(vertx, webClientOptions);
        vertx.deployVerticle(new MainVerticle(), new DeploymentOptions().setConfig(config), c -> async.complete());
    }

    @Test
    public void testIndex(TestContext context) {
        testUrlByJson(context, "/", json -> assertThat(json.getString("welcome"), containsString("一言")));
    }

    @Test
    public void testGushiciJson1(TestContext context) {
        testUrlByJson(context, "/all", json -> {
            assertThat(json.getString("content"), not(isEmptyString()));
        });
    }

    @Test
    public void testGushiciJson2(TestContext context) {
        testUrlByJson(context, "/all.json", json -> {
            assertThat(json.getString("content"), not(isEmptyString()));
        });
    }

    @Test
    public void testGushiciJson3(TestContext context) {
        testUrlByJson(context, "/dongwu", json -> {
            assertThat(json.getString("category"), containsString("动物"));
        });
    }

    @Test
    public void testGushiciJson4(TestContext context) {
        testUrlByJson(context, "/dongwu/xiema", json -> {
            assertThat(json.getString("category"), containsString("动物-写马"));
        });
    }

    @Test
    public void testGushiciJson5(TestContext context) {
        testUrlByJson(context, "/dongwu/404", json -> {
            assertThat(json.getInteger("error-code"), equalTo(404));
            assertThat(json.getString("reason"), containsString("没有结果"));
        });
    }

    @Test
    public void testGushiciPng(TestContext context) {
        testUrlByString(context, "/dongwu/xiema.png", str -> {
            assertThat(str, containsString("PNG"));
        });
    }

    @Test
    public void testGushiciSvg(TestContext context) {
        testUrlByString(context, "/dongwu/xiema.svg", str -> {
            assertThat(str, containsString("svg"));
        });
    }

    @Test
    public void testGushiciSvgParams1(TestContext context) {
        testUrlByString(context, "/dongwu/xiema.svg?font-size=8&spacing=5", str -> {
            assertThat(str, containsString("font-size=\"8.0\""));
            assertThat(str, containsString("letter-spacing=\"5.0\""));
        });
    }

    @Test
    public void testGushiciSvgParams2(TestContext context) {
        testUrlByString(context, "/dongwu/xiema.svg?font-size=7&spacing=5", str -> {
            assertThat(str, containsString("font-size=\"20.0\""));
            assertThat(str, containsString("letter-spacing=\"5.0\""));
        });
    }

    @Test
    public void testGushiciSvgParams3(TestContext context) {
        testUrlByString(context, "/dongwu/xiema.svg?font-size=100&spacing=70", str -> {
            assertThat(str, containsString("font-size=\"20.0\""));
            assertThat(str, containsString("letter-spacing=\"1.5\""));
        });
    }

    @Test
    public void testGushiciText(TestContext context) {
        testUrlByString(context, "/dongwu/xiema.txt", str -> {
            assertThat(str, not(isEmptyString()));
        });
    }

    @Test
    public void test404(TestContext context) {
        testUrlByJson(context, "/404", json -> {
            assertThat(json.getInteger("error-code"), equalTo(404));
        });
    }

    @Test
    public void testFavicon(TestContext context) {
        Async async = context.async();
        webClient.get("/favicon.ico").send(r -> {
            if (r.succeeded()) {
                context.assertEquals(404, r.result().statusCode());
                async.complete();
            } else {
                context.fail(r.cause());
            }
        });
    }

    @Test
    public void testLog(TestContext context) {
        Async async = context.async();
        webClient.get("/log").send(r1 -> {
            if (r1.succeeded()) {
                JsonObject o = r1.result().bodyAsJsonObject();
                webClient.get("/all").send(r2 -> {
                    if (r2.succeeded()) {
                        webClient.get("/log").send(r3 -> {
                            if (r3.succeeded()) {
                                JsonObject n = r3.result().bodyAsJsonObject();
                                // log 页面自身也计数
                                context.assertEquals(2,
                                        Integer.parseInt(n.getString("总点击量"))
                                                - Integer.parseInt(o.getString("总点击量")));
                                context.assertEquals(2,
                                        Integer.parseInt(n.getJsonArray("最近七天点击量").getString(0))
                                                - Integer.parseInt(o.getJsonArray("最近七天点击量").getString(0)));
                                async.complete();
                            } else {
                                context.fail(r3.cause());
                            }
                        });
                    } else {
                        context.fail(r2.cause());
                    }
                });
            } else {
                context.fail(r1.cause());
            }
        });
    }


    private void testUrlByJson(TestContext context, String uri, Consumer<JsonObject> assertCode) {
        testUrl(context, uri, HttpResponse::bodyAsJsonObject, assertCode);
    }

    private void testUrlByString(TestContext context, String uri, Consumer<String> assertCode) {
        testUrl(context, uri, HttpResponse::bodyAsString, assertCode);
    }


    private <T> void testUrl(TestContext context, String uri,
                             Function<HttpResponse<Buffer>, T> converter,
                             Consumer<T> assertCode) {
        Async async = context.async();
        webClient.get(uri).send(res -> {
            if (res.succeeded()) {
                T result = converter.apply(res.result());
                context.verify(v -> {
                    assertCode.accept(result);
                    async.complete();
                });
            } else {
                context.fail(res.cause());
            }
        });
    }
}
