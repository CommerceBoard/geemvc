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

package com.geemvc.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.geemvc.i18n.notice.Notices;
import com.geemvc.RequestContext;
import com.geemvc.ThreadStash;
import com.geemvc.bind.MethodParams;
import com.geemvc.converter.ConverterContext;
import com.geemvc.handler.CompositeControllerResolver;
import com.geemvc.handler.CompositeHandlerResolver;
import com.geemvc.handler.RequestHandler;
import com.geemvc.matcher.PathMatcherKey;
import com.geemvc.mock.bean.Person;
import com.geemvc.mock.controller.TestController18;
import org.junit.Test;

import com.geemvc.bind.MethodParam;
import com.geemvc.test.BaseTest;

public class ValidationTest extends BaseTest {
    @Test
    public void testFindController18a() {
        Errors e = instance(Errors.class);
        Notices n = instance(Notices.class);

        Map<String, String[]> requestParams = new HashMap<>();
        requestParams.put("person.forename", new String[] { "Michael" });
        requestParams.put("person.surname", new String[] { "Delamere" });
        requestParams.put("person.age", new String[] { "10" });

        RequestContext reqCtx = newRequestContext("/webapp", "/servlet", "/webapp/servlet/controller18/createPerson", "POST", requestParams);
        ThreadStash.prepare((HttpServletRequest) reqCtx.getRequest());
        ThreadStash.put(Errors.class, e);
        ThreadStash.put(Notices.class, n);

        CompositeHandlerResolver compositeHandlerResolver = instance(CompositeHandlerResolver.class);
        CompositeControllerResolver controllerResolver = instance(CompositeControllerResolver.class);
        MethodParams methodParams = instance(MethodParams.class);

        Map<PathMatcherKey, Class<?>> controllers = controllerResolver.resolve(reqCtx);
        RequestHandler requestHandler = compositeHandlerResolver.resolve(reqCtx, controllers.values());
        reqCtx.requestHandler(requestHandler);

        List<MethodParam> params = methodParams.get(requestHandler, reqCtx);
        Map<String, List<String>> requestValues = methodParams.values(params, reqCtx, e, n);

        Map<String, Object> typedValues = methodParams.typedValues(requestValues, params, reqCtx, e, n);

        ValidationContext validationCtx = injector.getInstance(ValidationContext.class).build(reqCtx, typedValues, n);

        Object o = instance(Validator.class).validate(requestHandler, validationCtx, e);

        requestHandler.invoke(typedValues);

        assertNotNull(requestHandler);
        assertNotNull(requestHandler.handlerMethod());
        assertNotNull(requestHandler.pathMatcher());
        assertEquals(requestHandler.controllerClass(), TestController18.class);
        assertEquals("createPerson", requestHandler.handlerMethod().getName());
        assertNotNull(params);
        assertNotNull(requestValues);
        assertNotNull(typedValues);
        assertEquals(2, params.size());
        assertEquals(2, requestValues.size());
        assertEquals(2, typedValues.size());
        assertEquals(Person.class, typedValues.get("person").getClass());
        assertEquals("Michael", ((Person) typedValues.get("person")).getForename());
        assertEquals("Delamere", ((Person) typedValues.get("person")).getSurname());
        assertEquals(10, ((Person) typedValues.get("person")).getAge());
    }

}
