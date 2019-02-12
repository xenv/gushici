package ma.luan.yiyan.util;

import java.util.*;

/**
 * 前缀树变种，用来加快分类获取的速度
 */
public class CategoryTrie {
    private Map<String, CategoryTrie> dict = new HashMap<>();
    private List<String> keys = new ArrayList<>(); // 当前 Trie 节点的子节点的所有 key
    private static CategoryTrie EMPTY_INSTANCE = new CategoryTrie();

    /**
     * 插入前缀树
     * @param categoryKey  example: img:shenghuo:buyi
     */
    public void insert(String categoryKey) {
        if (categoryKey == null || categoryKey.length() == 0) return;
        String[] categories = categoryKey.split(":");
        CategoryTrie currentTrie = this;
        for (String current : categories) {
            CategoryTrie childTrie = currentTrie.getDict().get(current);
            if (childTrie == null) {
                CategoryTrie newTrie = new CategoryTrie();
                newTrie.addKey(categoryKey);
                currentTrie.getDict().put(current, newTrie);
            } else {
                childTrie.addKey(categoryKey);
            }
            currentTrie  = currentTrie.getDict().get(current);
        }
    }

    /**
     * 搜索前缀树
     * @param categories example: [img]
     * @return [img:test:qq, img:test:aa, img:xx:aa]
     */
    public List<String> getKeys(Iterable categories) {
        CategoryTrie currentTrie = this;
        if (categories == null) {
            return new ArrayList<>();
        }
        for (Object category : categories) {
            if (category instanceof String) {
                currentTrie = currentTrie.getDict().getOrDefault(category, EMPTY_INSTANCE);
            }
        }
        return currentTrie.keys;
    }


    private Map<String, CategoryTrie> getDict() {
        return dict;
    }

    private void addKey(String key) {
        this.keys.add(key);
    }
}
