package ma.luan.yiyan.util;

import io.vertx.core.json.JsonArray;

import java.util.stream.Collector;

public class JsonCollector {
    public static Collector<Object, JsonArray, JsonArray>  toJsonArray() {
         return Collector.of(
            JsonArray::new,
            JsonArray::add,
            JsonArray::add
        );
    }
}
