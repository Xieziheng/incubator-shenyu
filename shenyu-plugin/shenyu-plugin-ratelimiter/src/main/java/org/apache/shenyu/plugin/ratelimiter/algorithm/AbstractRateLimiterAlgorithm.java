/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shenyu.plugin.ratelimiter.algorithm;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The type Abstract rate limiter algorithm.
 */
public abstract class AbstractRateLimiterAlgorithm implements RateLimiterAlgorithm<List<Long>> {

    /**
     * 是否初始化过
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    
    private RedisScript<List<Long>> script;
    
    /**
     * Gets script name.
     *
     * @return the script name
     */
    protected abstract String getScriptName();
    
    /**
     * Gets key name.
     *
     * @return the key name
     */
    protected abstract String getKeyName();

    /**
     * 默认脚本实现，从固定路径中读取lua脚本
     * @return
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RedisScript<List<Long>> getScript() {
        //未初始化则加载lua脚本到内存
        if (!this.initialized.get()) {
            DefaultRedisScript redisScript = new DefaultRedisScript<>();
            //算法脚本路径
            String scriptPath = "/META-INF/scripts/" + getScriptName();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(scriptPath)));
            redisScript.setResultType(List.class);
            this.script = redisScript;
            //cas设置为已初始化
            initialized.compareAndSet(false, true);
            return redisScript;
        }
        return script;
    }
    
    @Override
    public List<String> getKeys(final String id) {
        String prefix = getKeyName() + ".{" + id;
        String tokenKey = prefix + "}.tokens";
        String timestampKey = prefix + "}.timestamp";
        return Arrays.asList(tokenKey, timestampKey);
    }
}
