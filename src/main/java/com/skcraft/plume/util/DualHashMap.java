package com.skcraft.plume.util;

import java.util.HashMap;

public class DualHashMap<K, V> {
    private HashMap<K, V> hashMapOne;
    private HashMap<V, K> hashMapTwo;


    public DualHashMap() {
        hashMapOne = new HashMap<>();
        hashMapTwo = new HashMap<>();
    }

    public void put(K key, V val) {
        hashMapOne.put(key, val);
        hashMapTwo.put(val, key);
    }

    public void del(Object key) {
        hashMapOne.remove(key);
        hashMapTwo.remove(key);
    }

    public Object get(Object key) {
        if (hashMapOne.containsKey(key)) return hashMapOne.get(key);
        else if (hashMapTwo.containsKey(key)) return hashMapTwo.get(key);
        else return null;
    }

    public V getByKey(K key) {
        return hashMapOne.get(key);
    }

    public K getByVal(V key) {
        return hashMapTwo.get(key);
    }

    public boolean containsKey(K key) {
        return hashMapOne.containsKey(key);
    }

    public boolean containsValue(V val) {
        return hashMapOne.containsValue(val);
    }
}
