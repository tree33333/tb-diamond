/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.utils;

import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-6-15 ÉÏÎç09:35:32
 */

public class SoftReferenceHashMap<K, V> implements Serializable {
    static final long serialVersionUID = 3578712168613500464L;
    private Map<K, SoftValue> map;
    private transient ReferenceQueue<V> queue = new ReferenceQueue<V>();


    public SoftReferenceHashMap() {
        this.map = new HashMap<K, SoftValue>();
    }


    public SoftReferenceHashMap(int initialCapacity) {
        this.map = new HashMap<K, SoftValue>(initialCapacity);
    }


    public boolean containsKey(K key) {
        expungeStaleValues();
        return this.map.containsKey(key);
    }


    public V put(K key, V value) {
        expungeStaleValues();
        SoftValue softValue = new SoftValue(value, queue, key);
        SoftValue old = map.put(key, softValue);
        if (old != null) {
            return old.get();
        }
        else {
            return null;
        }
    }


    public V get(K key) {
        expungeStaleValues();
        SoftValue softValue = map.get(key);
        if (softValue != null) {
            return softValue.get();
        }
        else {
            return null;
        }
    }


    public boolean isEmpty() {
        expungeStaleValues();
        return map.isEmpty();
    }


    public int size() {
        expungeStaleValues();
        return map.size();
    }


    public void clear() {
        expungeStaleValues();
        map.clear();
    }


    public void remove(K key) {
        expungeStaleValues();
        map.remove(key);
    }


    private void expungeStaleValues() {
        SoftValue softValue;
        while ((softValue = (SoftValue) queue.poll()) != null) {
            K staleKey = softValue.key;
            map.remove(staleKey);
            softValue.clear();
            softValue = null; // help gc
        }
    }

    class SoftValue extends SoftReference<V> {
        K key;


        public SoftValue(V referent, ReferenceQueue<V> q, K key) {
            super(referent, q);
            this.key = key;
        }

    }
}
