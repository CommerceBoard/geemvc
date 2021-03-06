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

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.geemvc.Char;
import com.geemvc.RequestContext;
import com.geemvc.ThreadStash;
import com.geemvc.config.Configuration;
import com.geemvc.config.Configurations;
import com.geemvc.helper.Requests;
import com.geemvc.intercept.Interceptors;
import com.geemvc.intercept.LifecycleContext;
import com.geemvc.intercept.annotation.PostView;
import com.geemvc.intercept.annotation.PreView;
import com.geemvc.logging.Log;
import com.geemvc.logging.annotation.Logger;
import com.geemvc.view.bean.Result;
import com.geemvc.view.binding.Bindable;
import com.geemvc.view.binding.BindingContext;
import com.geemvc.view.binding.BindingResolver;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class DefaultViewHandler implements ViewHandler {
    protected final ViewAdapterFactory viewAdapterFactory;
    protected final StreamViewHandler streamViewHandler;
    protected final Requests requests;
    protected final Interceptors interceptors;
    protected final BindingResolver bindingResolver;

    @Inject
    protected Injector injector;

    @Logger
    protected Log log;

    protected Configuration configuration = Configurations.get();

    @Inject
    protected DefaultViewHandler(ViewAdapterFactory viewAdapterFactory, StreamViewHandler streamViewHandler, Requests requests, Interceptors interceptors, BindingResolver bindingResolver) {
        this.viewAdapterFactory = viewAdapterFactory;
        this.streamViewHandler = streamViewHandler;
        this.requests = requests;
        this.interceptors = interceptors;
        this.bindingResolver = bindingResolver;
    }

    @Override
    public void handle(Result result, RequestContext requestCtx) throws ServletException, IOException {
        if (result.view() != null) {
            String viewPath = viewPath(result.view());

            ViewAdapter viewAdapter = viewAdapterFactory.create(viewPath);

            if (viewAdapter == null)
                throw new IllegalStateException("No ViewAdapter found for the view-path '" + viewPath + "'. Please check your view-path or your configuration settings 'view-prefix' and 'view-suffix'.");

            log.debug("Processing forward view '{}' with adapter '{}'.", () -> viewPath, () -> viewAdapter.getClass().getName());

            LifecycleContext lifecycleCtx = (LifecycleContext) ThreadStash.get(LifecycleContext.class);

            processIncomingFlashVars(requestCtx);
            processViewBinders(result, requestCtx, lifecycleCtx);

            // ---------- Intercept lifecycle: PreView.
            interceptors.interceptLifecycle(PreView.class, lifecycleCtx);

            log.trace("Preparing data before forwarding request to view servlet.");
            viewAdapter.prepare(result, requestCtx);

            log.trace("Forwarding request to view servlet.");
            viewAdapter.forward(viewPath, requestCtx);

            // ---------- Intercept lifecycle: PostView.
            interceptors.interceptLifecycle(PostView.class, lifecycleCtx);

        } else if (result.redirect() != null) {
            HttpServletRequest request = (HttpServletRequest) requestCtx.getRequest();
            final String redirectPath = requests.toRequestURL(result.redirect(), request.isSecure(), request);

            processOutgoingFlashVars(result, requestCtx);

            log.debug("Sending user to redirect path '{}'.", () -> redirectPath);
            ((HttpServletResponse) requestCtx.getResponse()).sendRedirect(requests.toRequestURL(result.redirect(), request.isSecure(), request));
        } else if (result.status() != null) {
            if (result.message() != null)
                ((HttpServletResponse) requestCtx.getResponse()).sendError(result.status());
            else
                ((HttpServletResponse) requestCtx.getResponse()).sendError(result.status(), result.message());
        }
        // Assuming stream.
        else {
            log.debug("Streaming data to user for path '{}'.", () -> requestCtx.getPath());
            streamViewHandler.handle(result, requestCtx);
        }
    }

    protected void processViewBinders(Result result, RequestContext requestCtx, LifecycleContext lifecycleCtx) {
        HttpServletRequest request = (HttpServletRequest) requestCtx.getRequest();

        BindingContext bindingCtx = injector.getInstance(BindingContext.class).build(requestCtx.requestHandler(), requestCtx, lifecycleCtx.errors(), lifecycleCtx.notices())
                .bindings(lifecycleCtx.bindings())
                .result(result);

        Set<Bindable> viewBindings = bindingResolver.resolveBindings(bindingCtx);

        if (viewBindings != null && !viewBindings.isEmpty()) {
            for (Bindable viewBinding : viewBindings) {
                log.debug("Binding variable '{}' from '{}' for path '{}'.", () -> viewBinding.key(), () -> viewBinding.getClass().getName(), () -> result.view());
                request.setAttribute(viewBinding.key(), viewBinding.value());
            }
        }
    }

    protected void processIncomingFlashVars(RequestContext requestCtx) {
        HttpSession session = requestCtx.getSession(false);
        ServletRequest request = requestCtx.getRequest();

        if (session == null)
            return;

        Map<String, Object> flashVars = (Map<String, Object>) session.getAttribute(GeemvcKey.FLASH_VARS);
        session.removeAttribute(GeemvcKey.FLASH_VARS);

        if (flashVars != null && !flashVars.isEmpty()) {
            for (Map.Entry<String, Object> flashVar : flashVars.entrySet()) {
                log.debug("Recovering flash variable '{}' for path '{}'.", () -> flashVar.getKey(), () -> requestCtx.getPath());
                request.setAttribute(flashVar.getKey(), flashVar.getValue());
            }
        }
    }

    protected void processOutgoingFlashVars(Result view, RequestContext requestCtx) {
        Map<String, Object> flashVars = view.flashMap();

        if (flashVars == null || flashVars.size() == 0)
            return;

        log.debug("Adding flash variables {} to http session for redirect '{}'.", () -> flashVars.keySet(), () -> requestCtx.getPath() + " -> " + view.redirect());

        HttpSession session = requestCtx.getSession(true);
        session.setAttribute(GeemvcKey.FLASH_VARS, flashVars);
    }

    protected String viewPath(String path) {
        return path.startsWith("/WEB-INF/") ? path : new StringBuilder("/WEB-INF").append(configuration.viewPrefix()).append(Char.SLASH).append(path).append(configuration.viewSuffix()).toString();
    }
}
