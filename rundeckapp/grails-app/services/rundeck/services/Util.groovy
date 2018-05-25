/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck.services

import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry
import com.google.common.cache.Cache

/**
 * Created by greg on 7/30/15.
 */
class Util {

    static void addCacheMetrics(String name, MetricRegistry registry,Cache sourceCache) {
        registry?.register(
                MetricRegistry.name(name, "hitCount"),
                new Gauge<Long>() {
                    @Override
                    Long getValue() {
                        sourceCache.stats().hitCount()
                    }
                }
        )

        registry?.register(
                MetricRegistry.name(name, "evictionCount"),
                new Gauge<Long>() {
                    @Override
                    Long getValue() {
                        sourceCache.stats().evictionCount()
                    }
                }
        )
        registry?.register(
                MetricRegistry.name(name, "missCount"),
                new Gauge<Long>() {
                    @Override
                    Long getValue() {
                        sourceCache.stats().missCount()
                    }
                }
        )
        registry?.register(
                MetricRegistry.name(name, "loadExceptionCount"),
                new Gauge<Long>() {
                    @Override
                    Long getValue() {
                        sourceCache.stats().loadExceptionCount()
                    }
                }
        )
        registry?.register(
                MetricRegistry.name(name, "hitRate"),
                new Gauge<Double>() {
                    @Override
                    Double getValue() {
                        sourceCache.stats().hitRate()
                    }
                }
        )
    }
}
