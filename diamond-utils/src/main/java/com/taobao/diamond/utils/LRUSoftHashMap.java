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

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * 
 *基于 LinkedHashMap实现的LRU缓存，但是被LRU替换出来的值并不是简单移除，而是转成了软引用保存
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-6-16 下午08:05:27
 */

public class LRUSoftHashMap<K, V> extends LinkedHashMap<K, V> {

    public static final int DEFAULT_INITIAL_CAPACITY = 256;

    public static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private static final long serialVersionUID = 1846465456485877878L;

    private SoftReferenceHashMap<K, V> softMap = new SoftReferenceHashMap<K, V>();

    private int lowWaterMark, highWaterMark;


    // 不在此层做统计
    public double getHitRate() {
        return 0;
    }


    public LRUSoftHashMap(String name, int lowWaterMark, int highWaterMark) {
        super(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, true);
        this.lowWaterMark = lowWaterMark;
        this.highWaterMark = highWaterMark;
    }


    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        boolean result = this.hardSize() > getLowWaterMark();
        if (result) {
            // 没有超过highWaterMark的转变为软引用
            if (this.hardSize() + softMap.size() <= this.highWaterMark) {
                softMap.put(eldest.getKey(), eldest.getValue());
            }
        }
        return result;
    }


    @Override
    @SuppressWarnings("unchecked")
    public synchronized V get(Object key) {
        V result = super.get(key);
        if (result == null) {
            result = softMap.get((K) key);
        }
        return result;
    }


    @Override
    public synchronized void clear() {
        softMap.clear();
        super.clear();
    }


    @Override
    @SuppressWarnings("unchecked")
    public synchronized boolean containsKey(Object key) {
        return super.containsKey(key) || this.softMap.containsKey((K) key);
    }


    @Override
    public synchronized boolean isEmpty() {
        return super.isEmpty() && this.softMap.isEmpty();
    }


    @Override
    @SuppressWarnings("unchecked")
    public synchronized V remove(Object key) {
        softMap.remove((K) key);
        return super.remove(key);
    }


    @Override
    public synchronized final int size() {
        return softSize() + this.hardSize();
    }


    public synchronized final int hardSize() {
        return super.size();
    }


    public synchronized final int softSize() {
        return this.softMap.size();
    }


    public synchronized final int getLowWaterMark() {
        return lowWaterMark;
    }


    public synchronized final void setLowWaterMark(int lowWaterMark) {
        this.lowWaterMark = lowWaterMark;
    }


    public synchronized final int getHighWaterMark() {
        return highWaterMark;
    }


    public synchronized final void setHighWaterMark(int highWaterMark) {
        this.highWaterMark = highWaterMark;
    }


    public synchronized long getCurrentCacheSize() {
        return size();
    }

}
