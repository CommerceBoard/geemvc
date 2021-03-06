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

package com.geemvc.view;

import java.util.Map;

import com.geemvc.reflect.ReflectionProvider;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

@Singleton
public class DefaultViewAdapterFactory implements ViewAdapterFactory {
    protected final ReflectionProvider reflectionProvider;

    @Inject
    protected Injector inject;

    @Inject
    public DefaultViewAdapterFactory(ReflectionProvider reflectionProvider) {
        this.reflectionProvider = reflectionProvider;
    }

    @Override
    public ViewAdapter create(String viewPath) {
        Map<String, ViewAdapter> viewAdapters = reflectionProvider.locateViewAdapters();

        for (ViewAdapter viewAdapter : viewAdapters.values()) {
            if (viewAdapter.canHandle(viewPath)) {
                return viewAdapter;
            }
        }

        return null;
    }
}
