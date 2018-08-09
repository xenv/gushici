package ma.luan.yiyan.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import ma.luan.yiyan.constants.Key;
import ma.luan.yiyan.util.ConvertUtil;
import ma.luan.yiyan.util.JsonCollector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

public class ApiVerticle extends AbstractVerticle {
    private Logger log = LogManager.getLogger(this.getClass());

    @Override
    public void start(Future<Void> startFuture) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.get("/*").handler(this::log); // 全局日志处理，会执行 next() 到下一个
        router.get("/").handler(this::handleRoot); // 首页
        router.get("/favicon.ico").handler(c -> c.fail(404)); // 针对浏览器返回404
        router.get("/log").handler(this::showLog); // 显示日志
        router.routeWithRegex("/([a-z0-9/]*)\\.?(txt|json|png|svg|)")
            .handler(this::handleGushici); // 核心API调用
        router.route().last().handler(c -> c.fail(404)) // 其他返回404
            .failureHandler(this::returnError); // 对上面所有的错误进行处理
        vertx
            .createHttpServer()
            .requestHandler(router::accept)
            .listen(
                config().getInteger("http.port", 8080),
                result -> {
                    if (result.succeeded()) {
                        startFuture.complete();
                    } else {
                        startFuture.fail(result.cause());
                    }
                }
            );
    }


    private void handleRoot(RoutingContext routingContext) {
        JsonObject result = new JsonObject();
        result.put("welcome", "欢迎使用古诗词·一言");
        result.put("api-document", "下面为本API可用的所有类型，使用时，在链接最后面加上 .svg / .txt / .json / .png 可以获得不同格式的输出");
        result.put("help", "具体安装方法请访问项目首页 " + config().getString("index.url", "http://localhost/"));
        vertx.eventBus().<JsonArray>send(Key.GET_HELP_FROM_REDIS, null, res -> {
            if (res.succeeded()) {
                result.put("list", res.result().body());
                returnJsonWithCache(routingContext, result);
            } else {
                routingContext.fail(res.cause());
            }
        });
    }

    private void handleGushici(RoutingContext routingContext) {
        // 这里有两层回调，因为第二层回调需要用到第一层回调的数据。
        parseURI(routingContext) // 获取到 URI 上面的参数
            .setHandler(params -> {
                if (params.succeeded()) {
                    vertx.eventBus().<String>send(Key.GET_GUSHICI_FROM_REDIS, params.result(), res -> { // 从 Redis 拿数据
                        if (res.succeeded()) {
                            returnGushici(routingContext, res.result().body(), params.result());
                        } else {
                            routingContext.fail(res.cause());
                        }
                    });
                } else {
                    routingContext.fail(params.cause());
                }
            });
    }

    private void showLog(RoutingContext routingContext) {
        vertx.eventBus().<JsonObject>send(Key.GET_HISTORY_FROM_REDIS, null, res -> {
            if (res.succeeded()) {
                returnJson(routingContext, res.result().body());
            } else {
                routingContext.fail(res.cause());
            }
        });
    }


    private void returnJson(RoutingContext routingContext, JsonObject jsonObject) {
        setCommonHeader(routingContext.response()
            .putHeader("content-type", "application/json; charset=utf-8"))
            .end(jsonObject.encodePrettily());
    }

    private void returnJsonWithCache(RoutingContext routingContext, JsonObject jsonObject) {
        routingContext.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(jsonObject.encodePrettily());
    }

    private void returnError(RoutingContext routingContext) {
        JsonObject result = new JsonObject();
        int errorCode = routingContext.statusCode() > 0 ? routingContext.statusCode() : 500;
        // 不懂 Vert.x 为什么 EventBus 和 Web 是两套异常系统
        if (routingContext.failure() instanceof ReplyException) {
            errorCode = ((ReplyException) routingContext.failure()).failureCode();
        }
        result.put("error-code", errorCode);
        if (routingContext.failure() != null) {
            result.put("reason", routingContext.failure().getMessage());
        }
        setCommonHeader(routingContext.response()
            .setStatusCode(errorCode)
            .putHeader("content-type", "application/json; charset=utf-8"))
            .end(result.encodePrettily());
    }

    /*
     * 根据不同的 format 选择不同的返回策略
     */
    private void returnGushici(RoutingContext routingContext, String obj, JsonObject params) {
        switch (params.getString("format")) {
            case "json": {
                returnJson(routingContext, new JsonObject(obj));
                break;
            }
            case "svg": {
                setCommonHeader(routingContext.response()
                    .putHeader("Content-Type", "image/svg+xml; charset=utf-8"))
                    .end(ConvertUtil.getSvg(new JsonObject(obj).getString("content"),
                                params.getDouble("font-size"),
                                params.getDouble("spacing")));
                break;
            }
            case "txt": {
                setCommonHeader(routingContext.response()
                    .putHeader("Content-Type", "text/plain; charset=utf-8"))
                    .end(new JsonObject(obj).getString("content"));
                break;
            }
            case "png": {
                ConvertUtil.getImageFromBase64(obj).setHandler(res -> {
                    if (res.succeeded()) {
                        setCommonHeader(routingContext.response()
                            .putHeader("Content-Type", "image/png"))
                            .putHeader("Content-Length", res.result().length() + "")
                            .write(res.result()).end();
                    } else {
                        routingContext.fail(res.cause());
                    }
                });
                break;
            }
            default:
                routingContext.fail(new ReplyException(ReplyFailure.RECIPIENT_FAILURE, 400, "参数错误"));
        }
    }

    private HttpServerResponse setCommonHeader(HttpServerResponse response) {
        return response
            .putHeader("Access-Control-Allow-Origin", "*")
            .putHeader("Cache-Control", "no-cache");
    }

    /*
     * 记录点击量，只需要 publish 上 bus 即可
     */
    private void log(RoutingContext routingContext) {
        vertx.eventBus().publish(Key.SET_HISTORY_TO_REDIS, null);
        routingContext.next();
    }

    /**
     * 根据 uri 获取参数
     *
     * @param routingContext example: uri: /shenghuo/buyi.png , /all
     * @return {format: "png", categories: [shenghuo, buyi]}, {format:"json", categories:[""]}
     */
    private Future<JsonObject> parseURI(RoutingContext routingContext) {
        Future<JsonObject> result = Future.future();

        String rawCategory = routingContext.request().getParam("param0");
        String rawFormat = routingContext.request().getParam("param1");
        // 如果是 "all" 则当没有分类处理
        JsonArray categories = Arrays.stream(rawCategory.split("/"))
            .filter(s -> !s.isEmpty())
            .filter(s -> !"all".equals(s))
            .collect(JsonCollector.toJsonArray());
        // 默认 json
        String format = "".equals(rawFormat) ? "json" : rawFormat;

        JsonObject pathParams = new JsonObject();

        // svg 额外配置
        if ("svg".equals(format)) {
            HttpServerRequest request = routingContext.request();
            parseAndSet(pathParams,"font-size", request.getParam("font-size")
                    , 20, 8, 50);
            parseAndSet(pathParams,"spacing", request.getParam("spacing")
                    , 1.5, 0, 30);
        }

        pathParams.put("categories", categories);
        pathParams.put("format", format);
        result.complete(pathParams);
        return result;
    }

    private void parseAndSet(JsonObject jsonObject, String paramName,
                             String value, double defaultValue, double minValue, double maxValue) {
        if (value == null) {
            jsonObject.put(paramName, defaultValue);
        } else {
            try {
                double i = Double.parseDouble(value);
                if (Double.compare(i, minValue) >= 0 && Double.compare(i, maxValue) <= 0) {
                    jsonObject.put(paramName, i);
                } else {
                    jsonObject.put(paramName, defaultValue);
                }
            } catch (NumberFormatException ex) {
                jsonObject.put(paramName, defaultValue);
            }
        }
    }
}
