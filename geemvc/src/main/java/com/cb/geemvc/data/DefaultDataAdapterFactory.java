/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cb.geemvc.data;

import java.util.Map;

import com.cb.geemvc.reflect.ReflectionProvider;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class DefaultDataAdapterFactory implements DataAdapterFactory {
    protected final ReflectionProvider reflectionProvider;

    @Inject
    protected Injector inject;

    @Inject
    public DefaultDataAdapterFactory(ReflectionProvider reflectionProvider) {
        this.reflectionProvider = reflectionProvider;
    }

    @Override
    public DataAdapter create(Class<?> beanClass) {
        Map<String, DataAdapter> dataAdapters = reflectionProvider.locateDataAdapters();

        for (DataAdapter dataAdapter : dataAdapters.values()) {
            if (dataAdapter.canHandle(beanClass)) {
                return dataAdapter;
            }
        }

        return null;
    }
}
