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

package com.geemvc;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;

import com.geemvc.handler.HandlerResolutionPlan;
import com.geemvc.handler.RequestHandler;

public interface RequestContext {
    RequestContext build(ServletRequest request, ServletResponse response, ServletContext servletContext);

    RequestContext requestHandler(RequestHandler requestHandler);

    RequestHandler requestHandler();

    RequestContext add(RequestHandler requestHandler, HandlerResolutionPlan handlerResolutionPlan);

    HandlerResolutionPlan handlerResolutionPlan(RequestHandler requestHandler);

    String getPath();

    String getMethod();

    Map<String, String[]> getPathParameters();

    Map<String, String[]> getParameterMap();

    Map<String, String[]> getHeaderMap();

    Map<String, String[]> getCookieMap();

    Collection<String> getHeaderNames();

    String getHeader(String name);

    Collection<String> getHeaders(String name);

    String contentType();

    Collection<String> accepts();

    Collection<String> getAttributeNames();

    Object getAttribute(String name);

    void setAttribute(String name, Object value);

    Map<String, String> getCookies();

    ServletRequest getRequest();

    ServletResponse getResponse();

    ServletContext getServletContext();

    HttpSession getSession();

    HttpSession getSession(boolean create);

    Enumeration<Locale> getLocales();

    RequestContext currentLocale(Locale locale);

    Locale currentLocale();
}
