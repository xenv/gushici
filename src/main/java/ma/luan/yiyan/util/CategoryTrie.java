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
    public List<String> getKeys(List<String> categories) {
        CategoryTrie currentTrie = this;
        if (categories == null || categories.size() == 0) {
            return new ArrayList<>();
        }
        for (String category : categories) {
            currentTrie = currentTrie.getDict().getOrDefault(category, EMPTY_INSTANCE);
        }
        return currentTrie.keys;
    }


    private Map<String, CategoryTrie> getDict() {
        return dict;
    }

    private void addKey(String key) {
        this.keys.add(key);
    }


    // 测试
    public static void main(String[] args) {
        CategoryTrie root = new CategoryTrie();
        root.insert("img:test:qq");
        root.insert("img:test2:qq2");
        root.insert("img:test:qq2");
        root.insert("json:test2:qq");
        System.out.println(root.getKeys(Collections.singletonList("img")));
        System.out.println(root.getKeys(Arrays.asList("img","test")));
        System.out.println(root.getKeys(Arrays.asList("img","test2","qq2")));
    }
}