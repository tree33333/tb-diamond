/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.server.service;

import java.util.HashSet;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


/**
 * Redis服务，使用Redis的Java客户端Jedis
 * 
 * @author leiwen
 * 
 */
public class RedisService {

    private JedisPool pool;


    public RedisService(String redisServerIp, String redisServerPort) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxActive(100);
        poolConfig.setMaxWait(1000);
        pool = new JedisPool(poolConfig, redisServerIp, Integer.parseInt(redisServerPort));
    }


    /**
     * 增加一个value值到key对应的集合中
     */
    public void add(String key, String value) {
        Jedis client = null;
        try {
            client = pool.getResource();
            client.sadd(key, value);
            client.save();
        }
        finally {
            if (client != null) {
                client.disconnect();
                pool.returnResource(client);
            }
        }
    }


    /**
     * 增加一组value到key对应的集合中
     */
    public void addAll(String key, String[] values) {
        Jedis client = null;
        try {
            client = pool.getResource();
            for (String value : values) {
                client.sadd(key, value);
            }
            client.save();
        }
        finally {
            if (client != null) {
                client.disconnect();
                pool.returnResource(client);
            }
        }
    }


    /**
     * 将key对应的集合中的value值删除
     */
    public void remove(String key, String value) {
        Jedis client = null;
        try {
            client = pool.getResource();
            client.srem(key, value);
            client.save();
        }
        finally {
            if (client != null) {
                client.disconnect();
                pool.returnResource(client);
            }
        }
    }


    /*
     * 获取key对应的集合
     */
    public Set<String> get(String key) {
        Set<String> result = new HashSet<String>();
        Jedis client = null;
        try {
            client = pool.getResource();
            result = client.smembers(key);
            return result;
        }
        finally {
            if (client != null) {
                client.disconnect();
                pool.returnResource(client);
            }
        }
    }


    public void close() {
        pool.destroy();
    }
}
