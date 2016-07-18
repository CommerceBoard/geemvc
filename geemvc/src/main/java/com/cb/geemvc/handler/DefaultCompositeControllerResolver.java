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

package com.cb.geemvc.handler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.cb.geemvc.RequestContext;
import com.cb.geemvc.matcher.PathMatcherKey;
import com.cb.geemvc.reflect.ReflectionProvider;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

@Singleton
public class DefaultCompositeControllerResolver implements CompositeControllerResolver {

    protected final Set<ControllerResolver> controllerResolvers;

    protected final ReflectionProvider reflectionProvider;

    @Inject
    protected Injector injector;

    @Inject
    public DefaultCompositeControllerResolver(ReflectionProvider reflectionProvider) {
        this.reflectionProvider = reflectionProvider;
        this.controllerResolvers = reflectionProvider.locateControllerResolvers();
    }

    @Override
    public Map<PathMatcherKey, Class<?>> allControllers() {
        Map<PathMatcherKey, Class<?>> allControllers = new LinkedHashMap<>();

        for (ControllerResolver controllerResolver : controllerResolvers) {
            allControllers.putAll(controllerResolver.allControllers());
        }

        return allControllers;
    }

    @Override
    public Map<PathMatcherKey, Class<?>> resolve(RequestContext requestCtx) {
        Map<PathMatcherKey, Class<?>> resolvedControllers = new LinkedHashMap<>();

        for (ControllerResolver controllerResolver : controllerResolvers) {
            Map<PathMatcherKey, Class<?>> controllerClasses = controllerResolver.resolve(requestCtx);

            if (controllerClasses != null && !controllerClasses.isEmpty()) {
                resolvedControllers.putAll(controllerClasses);
            }
        }

        return resolvedControllers;
    }
}
