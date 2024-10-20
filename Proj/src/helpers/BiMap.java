package helpers;

import java.util.HashMap;

public class BiMap<K,V> extends HashMap<K,V> {

    private final HashMap<V,K> inverse;

    public BiMap() {
        super();
        inverse = new HashMap<>();
    }

    public void putByKey(K key, V value) {
        V prevValue = super.put(key, value);
        inverse.put(value, key);
    }

    public void putByValue(V value, K key) {
        V prevValue = super.put(key, value);
        inverse.put(value, key);
    }

    public V removeByKey(K key) {
        V value = super.remove(key);
        inverse.remove(value);
        return value;
    }

    public K removeByValue(V value) {
        K key = inverse.remove(value);
        super.remove(key);
        return key;
    }

    public void clear() {
        super.clear();
        inverse.clear();
    }

    public V getByKey(K key) {
        return super.get(key);
    }

    public K getByValue(V value) {
        return inverse.get(value);
    }
}
