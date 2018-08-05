package ma.luan.yiyan.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import ma.luan.yiyan.constants.Key;
import ma.luan.yiyan.util.ConvertUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiVerticle extends AbstractVerticle {
    private Logger log = LogManager.getLogger(this.getClass());

    @Override
    public void start(Future<Void> startFuture) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.get("/*").handler(this::log); // 全局日志处理，会执行 next() 到下一个
        router.get("/").handler(this::handleRoot); // 首页
        router.get("/favicon.ico").handler(c -> returnError(c, new Exception("404"))); // 针对浏览器返回404
        router.get("/log").handler(this::showLog); // 显示日志
        router.get("/*").handler(this::handleGushici); // 核心API调用
        router.route().last().handler(c -> { // 其他返回 404 （应该不会走到这里）
            returnError(c, new Exception("404"));
        });
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
        result.put("help", "具体安装方法请访问项目首页 https://gushi.ci");
        vertx.eventBus().<JsonArray>send(Key.GET_HELP_FROM_REDIS, null, res -> {
            if (res.succeeded()) {
                result.put("list", res.result().body());
                returnJsonWithCache(routingContext, result);
            } else {
                returnError(routingContext, res.cause());
            }
        });
    }

    private void handleGushici(RoutingContext routingContext) {
        // 这里有两层回调，因为第二层回调需要用到第一层回调的数据。
        parseURI(routingContext.normalisedPath()) // 获取到 URI 上面的参数
            .setHandler(params -> {
                if (params.succeeded()) {
                    vertx.eventBus().<String>send(Key.GET_GUSHICI_FROM_REDIS, params.result(), res -> { // 从 Redis 拿数据
                        if (res.succeeded()) {
                            returnGushici(routingContext, res.result().body(), params.result());
                        } else {
                            returnError(routingContext, res.cause());
                        }
                    });
                } else {
                    returnError(routingContext, params.cause());
                }
            });
    }

    private void showLog(RoutingContext routingContext) {
        vertx.eventBus().<JsonObject>send(Key.GET_HISTORY_FROM_REDIS, null, res -> {
            if (res.succeeded()) {
                returnJson(routingContext, res.result().body());
            } else {
                returnError(routingContext, res.cause());
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

    private void returnError(RoutingContext routingContext, Throwable cause) {
        JsonObject result = new JsonObject();
        result.put("error", cause.getMessage());
        int statusCode = cause.getMessage().startsWith("404") ? 404 : 500;
        if (statusCode == 500) {
            log.error(cause);
        }
        setCommonHeader(routingContext.response()
            .setStatusCode(statusCode)
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
                    .end(ConvertUtil.getSvg(new JsonObject(obj).getString("content")));
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
                        returnError(routingContext, res.cause());
                    }
                });
                break;
            }
            default:
                returnError(routingContext, new Exception("参数错误"));
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
     * @param uri 例如：/shenghuo/buyi.png, /all
     * @return {format: "png", classes: [shenghuo, buyi]}, {format:"json", classes:[""]}
     */
    private Future<JsonObject> parseURI(String uri) {
        Future<JsonObject> result = Future.future();
        if (uri.length() > 100) {
            result.fail(new IllegalArgumentException("参数太长了，别玩了"));
            return result;
        }

        JsonObject pathParams = new JsonObject();

        String rawClasses;
        String rawFormat = "";

        if (uri.contains(".")) {
            Pattern pattern = Pattern.compile("/(.*)\\.(.*)");
            Matcher m = pattern.matcher(uri);
            if (!m.find()) {
                result.fail(new IllegalArgumentException("非法参数"));
                return result;
            }
            rawClasses = m.group(1);
            rawFormat = m.group(2);
        } else {
            rawClasses = uri.replaceFirst("/", "");
        }


        String[] classes = rawClasses.split("/");
        if (classes.length < 1) {
            result.fail(new IllegalArgumentException("非法参数"));
            return result;
        }
        classes[0] = classes[0].replaceFirst("all", "");
        pathParams.put("classes", new JsonArray(Arrays.asList(classes)));

        // 处理文件后缀

        String format;
        if (Arrays.asList("json", "svg", "txt", "png", "").contains(rawFormat)) {
            format = "".equals(rawFormat) ? "json" : rawFormat;
        } else {
            result.fail(new IllegalArgumentException("非法参数"));
            return result;
        }
        pathParams.put("format", format);

        result.complete(pathParams);
        return result;
    }
}
