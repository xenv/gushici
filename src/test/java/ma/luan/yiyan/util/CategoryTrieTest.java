package ma.luan.yiyan.util;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class CategoryTrieTest {
    @Test
    public void testTrie() {
        CategoryTrie root = new CategoryTrie();
        root.insert("img:test:qq");
        assertThat(root.getKeys(Collections.singletonList("img")), hasSize(1));
        root.insert("");
        assertThat(root.getKeys(Collections.singletonList("img")), hasSize(1));
        root.insert("img:test2:qq2");
        root.insert("img:test:qq2");
        root.insert("json:test2:qq");
        assertThat(root.getKeys(Collections.singletonList("img")),
                Matchers.<List<String>>allOf(hasSize(3), hasItems(
                        "img:test:qq", "img:test2:qq2", "img:test:qq2")));
        assertThat(root.getKeys(Arrays.asList("img", "test")),
                Matchers.<List<String>>allOf(hasSize(2), hasItems("img:test:qq", "img:test:qq2")));
        assertThat(root.getKeys(Arrays.asList("img", "test2", "qq2")),
                Matchers.<List<String>>allOf(hasSize(1), hasItems("img:test2:qq2")));
        assertThat(root.getKeys(Collections.singletonList("json")),
                Matchers.<List<String>>allOf(hasSize(1), hasItems("json:test2:qq")));
        assertThat(root.getKeys(Collections.singletonList("qq")), hasSize(0));
        assertThat(root.getKeys(Collections.emptyList()), hasSize(0));
    }

}