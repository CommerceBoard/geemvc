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

package com.cb.geemvc.reflect;

import com.cb.geemvc.annotation.Adapter;
import com.cb.geemvc.annotation.Controller;
import com.cb.geemvc.annotation.Evaluator;
import com.cb.geemvc.annotation.Request;
import com.cb.geemvc.bind.param.ParamAdapter;
import com.cb.geemvc.bind.param.ParamAdapterKey;
import com.cb.geemvc.bind.param.TypedParamAdapter;
import com.cb.geemvc.cache.Cache;
import com.cb.geemvc.converter.ConverterAdapter;
import com.cb.geemvc.converter.ConverterAdapterKey;
import com.cb.geemvc.data.DataAdapter;
import com.cb.geemvc.handler.ControllerResolver;
import com.cb.geemvc.handler.HandlerResolver;
import com.cb.geemvc.handler.RequestMappingKey;
import com.cb.geemvc.helper.Annotations;
import com.cb.geemvc.i18n.message.MessageResolver;
import com.cb.geemvc.intercept.AroundHandler;
import com.cb.geemvc.intercept.LifecycleInterceptor;
import com.cb.geemvc.intercept.annotation.Intercept;
import com.cb.geemvc.validation.ValidationAdapter;
import com.cb.geemvc.validation.ValidationAdapterKey;
import com.cb.geemvc.validation.Validator;
import com.cb.geemvc.validation.annotation.CheckBean;
import com.cb.geemvc.view.ViewAdapter;
import com.google.inject.Inject;
import com.google.inject.Injector;

import javax.ws.rs.Path;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultReflectionProvider implements ReflectionProvider {
    protected final ReflectionsWrapper reflectionsWrapper;
    protected final Annotations annotations;
    protected final Cache cache;

    @Inject
    protected Injector injector;

    protected static final String CACHE_KEY_LOCATED_CONTROLLERS = "gee.located.controllers";
    protected static final String CACHE_KEY_LOCATED_EVALUATORS = "gee.located.evaluators";

    @Inject
    public DefaultReflectionProvider(ReflectionsWrapper reflectionsWrapper, Annotations annotations, Cache cache) {
        this.reflectionsWrapper = reflectionsWrapper;
        this.annotations = annotations;
        this.cache = cache;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<Class<?>> locateControllers() {
        Set<Class<?>> locatedControllers = (Set<Class<?>>) cache.get(CACHE_KEY_LOCATED_CONTROLLERS);

        if (locatedControllers == null) {
            Set<Class<?>> geeControllers = reflectionsWrapper.getTypesAnnotatedWith(Controller.class, true);
            Set<Class<?>> jsr311Controllers = reflectionsWrapper.getTypesAnnotatedWith(Path.class, true);

            locatedControllers = new LinkedHashSet<>(geeControllers);
            locatedControllers.addAll(jsr311Controllers);

//            Set<Class<?>> cachedLocatedControllers = (Set<Class<?>>)

                    cache.putIfAbsent(CACHE_KEY_LOCATED_CONTROLLERS, locatedControllers);

//            if (cachedLocatedControllers != null)
//                locatedControllers = cachedLocatedControllers;
        }

        return locatedControllers;
    }

    @Override
    public Set<ControllerResolver> locateControllerResolvers() {
        Set<ControllerResolver> controllerResolvers = new LinkedHashSet<>();

        Set<Class<?>> adapterClasses = reflectionsWrapper.getTypesAnnotatedWith(Adapter.class, true);

        List<Class<?>> controllerResolverClasses = new ArrayList<Class<?>>();

        for (Class<?> adapterClass : adapterClasses) {
            if (ControllerResolver.class.isAssignableFrom(adapterClass))
                controllerResolverClasses.add(adapterClass);
        }

        controllerResolverClasses.sort((cl1, cl2) -> cl1.getAnnotation(Adapter.class).weight() - cl2.getAnnotation(Adapter.class).weight());

        for (Class<?> controllerResolverClass : controllerResolverClasses) {
            ControllerResolver controllerResolver = (ControllerResolver) injector.getInstance(controllerResolverClass);
            controllerResolvers.add(controllerResolver);
        }

        return controllerResolvers;
    }

    @Override
    public Set<HandlerResolver> locateHandlerResolvers() {
        Set<HandlerResolver> handlerResolvers = new LinkedHashSet<>();

        Set<Class<?>> adapterClasses = reflectionsWrapper.getTypesAnnotatedWith(Adapter.class, true);

        List<Class<?>> handlerResolverClasses = new ArrayList<Class<?>>();

        for (Class<?> adapterClass : adapterClasses) {
            if (HandlerResolver.class.isAssignableFrom(adapterClass))
                handlerResolverClasses.add(adapterClass);
        }

        handlerResolverClasses.sort((cl1, cl2) -> cl1.getAnnotation(Adapter.class).weight() - cl2.getAnnotation(Adapter.class).weight());

        for (Class<?> handlerResolverClass : handlerResolverClasses) {
            HandlerResolver handlerResolver = (HandlerResolver) injector.getInstance(handlerResolverClass);
            handlerResolvers.add(handlerResolver);
        }

        return handlerResolvers;
    }

    @Override
    public Set<MessageResolver> locateMessageResolvers() {
        Set<MessageResolver> messageResolvers = new LinkedHashSet<>();

        Set<Class<?>> adapterClasses = reflectionsWrapper.getTypesAnnotatedWith(Adapter.class, true);

        List<Class<?>> messageResolverClasses = new ArrayList<Class<?>>();

        for (Class<?> adapterClass : adapterClasses) {
            if (MessageResolver.class.isAssignableFrom(adapterClass))
                messageResolverClasses.add(adapterClass);
        }

        messageResolverClasses.sort((cl1, cl2) -> cl1.getAnnotation(Adapter.class).weight() - cl2.getAnnotation(Adapter.class).weight());

        for (Class<?> messageResolverClass : messageResolverClasses) {
            MessageResolver messageResolver = (MessageResolver) injector.getInstance(messageResolverClass);
            messageResolvers.add(messageResolver);
        }

        return messageResolvers;
    }

    @Override
    public Set<AroundHandler> locateAroundHandlerInterceptors() {
        Set<AroundHandler> aroundHandlerInterceptors = new LinkedHashSet<>();

        Set<Class<?>> interceptClasses = reflectionsWrapper.getTypesAnnotatedWith(Intercept.class, true);

        List<Class<?>> aroundHandlerInterceptorClasses = new ArrayList<Class<?>>();

        for (Class<?> interceptClass : interceptClasses) {
            if (AroundHandler.class.isAssignableFrom(interceptClass))
                aroundHandlerInterceptorClasses.add(interceptClass);
        }

        aroundHandlerInterceptorClasses.sort((cl1, cl2) -> cl1.getAnnotation(Intercept.class).weight() - cl2.getAnnotation(Intercept.class).weight());

        for (Class<?> aroundHandlerInterceptorClass : aroundHandlerInterceptorClasses) {
            AroundHandler aroundHandler = (AroundHandler) injector.getInstance(aroundHandlerInterceptorClass);
            aroundHandlerInterceptors.add(aroundHandler);
        }

        return aroundHandlerInterceptors;
    }

    @Override
    public Set<LifecycleInterceptor> locateLifecycleInterceptors(Class<? extends Annotation> lifecycleAnnotation, Class<?> controllerClass) {
        Set<LifecycleInterceptor> lifecycleInterceptorInterceptors = new LinkedHashSet<>();
        Set<Method> lifecycleInterceptMethods = reflectionsWrapper.getMethodsAnnotatedWith(lifecycleAnnotation);

        for (Method lifecycleInterceptMethod : lifecycleInterceptMethods) {
            LifecycleInterceptor lifecycleInterceptor = injector.getInstance(LifecycleInterceptor.class).build(lifecycleInterceptMethod.getAnnotation(lifecycleAnnotation), lifecycleInterceptMethod);
            lifecycleInterceptorInterceptors.add(lifecycleInterceptor);
        }

        return lifecycleInterceptorInterceptors;
    }

    @Override
    public Set<LifecycleInterceptor> locateLifecycleInterceptors(Class<? extends Annotation> lifecycleAnnotation) {
        Set<LifecycleInterceptor> lifecycleInterceptorInterceptors = new LinkedHashSet<>();
        Set<Class<?>> lifecycleInterceptTypes = reflectionsWrapper.getTypesAnnotatedWith(lifecycleAnnotation);

        for (Class<?> lifecycleInterceptType : lifecycleInterceptTypes) {

            Method lifecycleInterceptMethod = interceptorMethod(lifecycleInterceptType);
            LifecycleInterceptor lifecycleInterceptor = injector.getInstance(LifecycleInterceptor.class).build(lifecycleInterceptType.getAnnotation(lifecycleAnnotation), lifecycleInterceptMethod);
            lifecycleInterceptorInterceptors.add(lifecycleInterceptor);
        }

        return lifecycleInterceptorInterceptors;
    }

    protected Method interceptorMethod(Class<?> lifecycleInterceptType) {
        Method[] methods = lifecycleInterceptType.getDeclaredMethods();

        Method publicMethod = null;

        for (Method method : methods) {
            int modifiers = method.getModifiers();

            if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
                if (publicMethod == null) {
                    publicMethod = method;
                } else {
                    throw new IllegalStateException("Only 1 public method should be specified in your interceptor '" + lifecycleInterceptType.getName() + "' so that Geemvc knows which one to call");
                }
            }
        }

        return publicMethod;
    }

    @Override
    public Set<Class<?>> locateEvaluators() {
        return reflectionsWrapper.getTypesAnnotatedWith(Evaluator.class, true);
    }

    @Override
    public Request getRequestAnnotation(Object o) {
        return o == null ? null : o.getClass().getAnnotation(Request.class);
    }

    @Override
    public Set<Method> locateHandlerMethods(Predicate<Method> predicate) {
        Set<Method> handlerMethods = reflectionsWrapper.getMethodsAnnotatedWith(Request.class);

        if (handlerMethods == null || handlerMethods.size() == 0)
            return null;

        return handlerMethods.stream().filter(predicate).collect(Collectors.toSet());
    }

    @Override
    public Map<RequestMappingKey, Method> getRequestHandlerMethods(Class<?> controllerClass) {
        return getRequestHandlerMethods(controllerClass, null);
    }

    @Override
    public Map<RequestMappingKey, Method> getRequestHandlerMethods(Class<?> controllerClass, Predicate<Entry<RequestMappingKey, Method>> predicate) {
        if (controllerClass == null)
            return null;

        Method[] methods = controllerClass.getMethods();

        Map<RequestMappingKey, Method> requestMappings = new LinkedHashMap<>();

        for (Method method : methods) {
            if (method.isAnnotationPresent(Request.class) || method.isAnnotationPresent(Path.class)) {
                requestMappings.put(injector.getInstance(RequestMappingKey.class).build(controllerClass, method, annotations.requestMapping(method)), method);
            }
        }

        Stream<Entry<RequestMappingKey, Method>> stream = requestMappings.entrySet().stream();

        if (predicate != null)
            stream = stream.filter(predicate);

        requestMappings = stream.sorted(Map.Entry.<RequestMappingKey, Method>comparingByKey((k1, k2) -> k2.requestMapping().priority() - k1.requestMapping().priority())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> {
            throw new AssertionError();
        }, LinkedHashMap::new));

        return requestMappings;
    }

    @Override
    public Map<ConverterAdapterKey, ConverterAdapter<?>> locateConverterAdapters() {
        Map<ConverterAdapterKey, ConverterAdapter<?>> converters = new LinkedHashMap<>();

        Set<Class<?>> adapterClasses = reflectionsWrapper.getTypesAnnotatedWith(Adapter.class, true);

        List<Class<?>> converterClasses = new ArrayList<Class<?>>();

        for (Class<?> adapterClass : adapterClasses) {
            if (ConverterAdapter.class.isAssignableFrom(adapterClass))
                converterClasses.add(adapterClass);
        }

        converterClasses.sort((cl1, cl2) -> cl1.getAnnotation(Adapter.class).weight() - cl1.getAnnotation(Adapter.class).weight());

        for (Class<?> converterClass : converterClasses) {
            Type[] inferfaces = converterClass.getGenericInterfaces();

            for (Type type : inferfaces) {
                if (type instanceof ParameterizedType) {
                    Type rawClass = ((ParameterizedType) type).getRawType();

                    if (rawClass == ConverterAdapter.class) {
                        List<Class<?>> genericType = getGenericType(type);

                        if (genericType.size() == 1) {
                            converters.put(injector.getInstance(ConverterAdapterKey.class).build(genericType.get(0)).weight(converterClass.getAnnotation(Adapter.class).weight()), (ConverterAdapter<?>) injector.getInstance(converterClass));
                        } else if (genericType.size() > 1) {
                            converters.put(injector.getInstance(ConverterAdapterKey.class).build(genericType.get(0), genericType.subList(1, genericType.size())).weight(converterClass.getAnnotation(Adapter.class).weight()),
                                    (ConverterAdapter<?>) injector.getInstance(converterClass));
                        }
                    }
                }
            }
        }

        Map<ConverterAdapterKey, ConverterAdapter<?>> sortedConverters = converters.entrySet().stream().sorted((e1, e2) -> e2.getKey().weight() - e1.getKey().weight())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        return sortedConverters;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<ParamAdapterKey, ParamAdapter<?>> locateParamAdapters() {
        Map<ParamAdapterKey, ParamAdapter<?>> paramAdapters = new HashMap<>();

        Set<Class<?>> adapterClasses = reflectionsWrapper.getTypesAnnotatedWith(Adapter.class, true);

        List<Class<?>> paramAdapterClasses = new ArrayList<Class<?>>();

        for (Class<?> adapterClass : adapterClasses) {
            if (ParamAdapter.class.isAssignableFrom(adapterClass))
                paramAdapterClasses.add(adapterClass);
        }

        paramAdapterClasses.sort((cl1, cl2) -> cl1.getAnnotation(Adapter.class).weight() - cl2.getAnnotation(Adapter.class).weight());

        for (Class<?> paramAdapterClass : paramAdapterClasses) {
            Type[] inferfaces = paramAdapterClass.getGenericInterfaces();

            for (Type type : inferfaces) {
                if (type instanceof ParameterizedType) {
                    Type rawClass = ((ParameterizedType) type).getRawType();

                    if (rawClass == ParamAdapter.class || rawClass == TypedParamAdapter.class) {
                        List<Class<?>> genericType = getGenericType(type);
                        paramAdapters.put(injector.getInstance(ParamAdapterKey.class).build((Class<? extends Annotation>) genericType.get(0)), (ParamAdapter<?>) injector.getInstance(paramAdapterClass));
                    }
                }
            }
        }

        return paramAdapters;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<ValidationAdapterKey, ValidationAdapter<?>> locateValidationAdapters() {
        Map<ValidationAdapterKey, ValidationAdapter<?>> validationAdapters = new HashMap<>();

        Set<Class<?>> adapterClasses = reflectionsWrapper.getTypesAnnotatedWith(Adapter.class, true);

        List<Class<?>> validationAdapterClasses = new ArrayList<Class<?>>();

        for (Class<?> adapterClass : adapterClasses) {
            if (ValidationAdapter.class.isAssignableFrom(adapterClass))
                validationAdapterClasses.add(adapterClass);
        }

        validationAdapterClasses.sort((cl1, cl2) -> cl1.getAnnotation(Adapter.class).weight() - cl2.getAnnotation(Adapter.class).weight());

        for (Class<?> validationAdapterClass : validationAdapterClasses) {
            Type[] inferfaces = validationAdapterClass.getGenericInterfaces();

            for (Type type : inferfaces) {
                if (type instanceof ParameterizedType) {
                    Type rawClass = ((ParameterizedType) type).getRawType();

                    if (rawClass == ValidationAdapter.class) {
                        List<Class<?>> genericType = getGenericType(type);
                        validationAdapters.put(injector.getInstance(ValidationAdapterKey.class).build((Class<? extends Annotation>) genericType.get(0)), (ValidationAdapter<?>) injector.getInstance(validationAdapterClass));
                    }
                }
            }
        }

        return validationAdapters;
    }

    @Override
    public Set<Validator> locateBeanValidators(Class<?> forType) {
        Set<Validator> validators = new LinkedHashSet<>();
        List<Class<?>> validatorClasses = new ArrayList<>(reflectionsWrapper.getTypesAnnotatedWith(CheckBean.class, true));

        validatorClasses.sort((cl1, cl2) -> cl1.getAnnotation(CheckBean.class).weight() - cl2.getAnnotation(CheckBean.class).weight());

        for (Class<?> validatorClass : validatorClasses) {
            CheckBean checkBean = validatorClass.getAnnotation(CheckBean.class);

            if (((checkBean.value() != Object.class && checkBean.value() == forType) || (checkBean.type() != Object.class && checkBean.type() == forType)) && Validator.class.isAssignableFrom(validatorClass)) {
                validators.add((Validator) injector.getInstance(validatorClass));
            }
        }

        return validators;
    }

    @Override
    public Map<String, ViewAdapter> locateViewAdapters() {
        Map<String, ViewAdapter> viewAdapters = new HashMap<>();

        Set<Class<?>> adapterClasses = reflectionsWrapper.getTypesAnnotatedWith(Adapter.class, true);

        List<Class<?>> viewAdapterClasses = new ArrayList<Class<?>>();

        for (Class<?> viewClass : adapterClasses) {
            if (ViewAdapter.class.isAssignableFrom(viewClass))
                viewAdapterClasses.add(viewClass);
        }

        viewAdapterClasses.sort((cl1, cl2) -> cl1.getAnnotation(Adapter.class).weight() - cl2.getAnnotation(Adapter.class).weight());

        for (Class<?> viewAdapterClass : viewAdapterClasses) {
            ViewAdapter viewAdapter = (ViewAdapter) injector.getInstance(viewAdapterClass);
            viewAdapters.put(viewAdapter.name(), viewAdapter);
        }

        return viewAdapters;
    }

    @Override
    public Map<String, DataAdapter> locateDataAdapters() {
        Map<String, DataAdapter> dataAdapters = new HashMap<>();

        Set<Class<?>> adapterClasses = reflectionsWrapper.getTypesAnnotatedWith(Adapter.class, true);

        List<Class<?>> dataAdapterClasses = new ArrayList<Class<?>>();

        for (Class<?> dataAdapterClass : adapterClasses) {
            if (DataAdapter.class.isAssignableFrom(dataAdapterClass))
                dataAdapterClasses.add(dataAdapterClass);
        }

        dataAdapterClasses.sort((cl1, cl2) -> cl1.getAnnotation(Adapter.class).weight() - cl2.getAnnotation(Adapter.class).weight());

        for (Class<?> dataAdapterClass : dataAdapterClasses) {
            DataAdapter dataAdapter = (DataAdapter) injector.getInstance(dataAdapterClass);
            dataAdapters.put(dataAdapter.name(), dataAdapter);
        }

        return dataAdapters;
    }

    @Override
    public List<Class<?>> getGenericType(Type[] inferfaces) {
        for (Type type : inferfaces) {
            if (type instanceof ParameterizedType) {
                Type rawClass = ((ParameterizedType) type).getRawType();

                if (rawClass == ConverterAdapter.class) {
                    return getGenericType(type);
                }
            }
        }

        return null;
    }

    @Override
    public List<Class<?>> getGenericType(Type genericType) {
        List<Class<?>> ret = null;

        if (genericType instanceof ParameterizedType) {
            Type[] argTypes = ((ParameterizedType) genericType).getActualTypeArguments();

            if (argTypes != null && argTypes.length > 0) {
                ret = new ArrayList<>();

                for (Type argType : argTypes) {
                    if (argType instanceof Class<?>) {
                        ret.add((Class<?>) argType);
                    } else if (argType instanceof ParameterizedType) {
                        ret.add((Class<?>) ((ParameterizedType) argType).getRawType());

                        Type[] nestedArgTypes = ((ParameterizedType) argType).getActualTypeArguments();

                        for (Type nestedArgType : nestedArgTypes) {
                            if (nestedArgType instanceof Class<?>) {
                                ret.add((Class<?>) nestedArgType);
                            } else if (nestedArgType instanceof ParameterizedType) {
                                ret.add((Class<?>) ((ParameterizedType) nestedArgType).getRawType());
                            }
                        }
                    }
                }
            }
        }

        return ret;
    }

    @Override
    public List<Class<?>> getGenericType(Class<?> type, String propertyName) {
        List<Class<?>> genericType = null;
        Field field = getField(type, propertyName);

        if (field != null) {
            genericType = getGenericType(field.getGenericType());
        } else {
            PropertyDescriptor pd = getPropertyDescriptor(type, propertyName);

            if (pd != null) {
                Method readMethod = pd.getReadMethod();

                if (readMethod != null) {
                    genericType = getGenericType(readMethod.getGenericReturnType());
                } else {
                    Method writeMethod = pd.getWriteMethod();

                    if (writeMethod != null)
                        genericType = getGenericType(writeMethod.getGenericReturnType());
                }
            }
        }

        return genericType;
    }

    @Override
    public Set<Field> getMutableFields(Class<?> clazz) {
        Set<Field> fields = new LinkedHashSet<>();
        Class<?> current = clazz;

        while (current != null && current != Object.class) {

            Field[] allFields = current.getDeclaredFields();

            for (Field field : allFields) {
                if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }

            current = current.getSuperclass();
        }

        return fields;
    }

    @Override
    public Set<Field> getFieldsAnnotatedWith(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        Set<Field> fields = new LinkedHashSet<>();
        Class<?> current = clazz;

        while (current != null && current != Object.class) {
            Field[] allFields = current.getDeclaredFields();

            for (Field field : allFields) {
                if (field.isAnnotationPresent(annotationClass)) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }

            current = current.getSuperclass();
        }

        return fields;
    }

    @Override
    public Field getField(Class<?> clazz, String name) {
        Field foundField = null;
        Class<?> current = clazz;

        while (current != null && current != Object.class) {
            Field[] allFields = current.getDeclaredFields();

            for (Field field : allFields) {
                if (field.getName().equals(name)) {
                    field.setAccessible(true);
                    foundField = field;
                    break;
                }
            }

            current = current.getSuperclass();
        }

        return foundField;
    }

    @Override
    public PropertyDescriptor getPropertyDescriptor(Class<?> beanType, String name) {
        try {
            PropertyDescriptor[] pds = Introspector.getBeanInfo(beanType).getPropertyDescriptors();

            for (PropertyDescriptor propertyDescriptor : pds) {
                if (propertyDescriptor.getName().equals(name)) {
                    return propertyDescriptor;
                }
            }
        } catch (IntrospectionException e) {
        }

        return null;
    }

    @Override
    public boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type.isEnum()
                || Number.class.isAssignableFrom(type)
                || CharSequence.class.isAssignableFrom(type)
                || Date.class.isAssignableFrom(type)
                || Locale.class.isAssignableFrom(type)
                || URI.class.isAssignableFrom(type)
                || URL.class.isAssignableFrom(type)
                || Class.class == type
                || (type.isArray() && isSimpleType(type.getComponentType()));
    }

    @Override
    public Class<?> getPrimaryType(Class<?> type, Type parameterizedType) {
        if (type.isArray()) {
            return type.getComponentType();

        } else if (Collection.class.isAssignableFrom(type) && parameterizedType != null) {
            List<Class<?>> genericType = getGenericType(parameterizedType);

            if (genericType == null || genericType.isEmpty())
                return null;

            if (genericType.size() == 1)
                return genericType.get(0);

            if (genericType.size() == 2 && Collection.class.isAssignableFrom(genericType.get(0)))
                return genericType.get(1);

            if (genericType.size() == 3 && Map.class.isAssignableFrom(genericType.get(0)))
                return genericType.get(2);

            return null;

        } else if (Map.class.isAssignableFrom(type) && parameterizedType != null) {
            List<Class<?>> genericType = getGenericType(parameterizedType);

            if (genericType.size() == 2)
                return genericType.get(1);

            if (genericType.size() == 3 && Collection.class.isAssignableFrom(genericType.get(1)))
                return genericType.get(2);

            return null;
        } else {
            return type;
        }
    }
}
