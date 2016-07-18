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

package com.cb.geemvc.view;

import com.cb.geemvc.Char;
import com.cb.geemvc.RequestContext;
import com.cb.geemvc.ThreadStash;
import com.cb.geemvc.config.Configuration;
import com.cb.geemvc.helper.Requests;
import com.cb.geemvc.intercept.Interceptors;
import com.cb.geemvc.intercept.LifecycleContext;
import com.cb.geemvc.intercept.annotation.PostView;
import com.cb.geemvc.intercept.annotation.PreView;
import com.cb.geemvc.view.bean.View;
import com.google.inject.Inject;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

public class DefaultViewHandler implements ViewHandler {
    protected final ViewAdapterFactory viewAdapterFactory;
    protected final StreamViewHandler streamViewHandler;
    protected final Requests requests;
    protected final Interceptors interceptors;

    @Inject
    protected Configuration configuration;

    @Inject
    protected DefaultViewHandler(ViewAdapterFactory viewAdapterFactory, StreamViewHandler streamViewHandler, Requests requests, Interceptors interceptors) {
        this.viewAdapterFactory = viewAdapterFactory;
        this.streamViewHandler = streamViewHandler;
        this.requests = requests;
        this.interceptors = interceptors;
    }

    @Override
    public void handle(View view, RequestContext requestCtx) throws ServletException, IOException {
        if (view.forward() != null) {
            String viewPath = viewPath(view.forward());

            ViewAdapter viewAdapter = viewAdapterFactory.create(viewPath);

            if (viewAdapter == null)
                throw new IllegalStateException("No ViewAdapter found for the view-path '" + viewPath + "'. Please check your view-path or your configuration settings 'view-prefix' and 'view-suffix'.");

            processIncomingFlashVars(requestCtx);

            // ---------- Intercept lifecycle: PreView.
            interceptors.interceptLifecycle(PreView.class, (LifecycleContext) ThreadStash.get(LifecycleContext.class));

            viewAdapter.prepare(view, requestCtx);

            viewAdapter.forward(viewPath, requestCtx);

            // ---------- Intercept lifecycle: PostView.
            interceptors.interceptLifecycle(PostView.class, (LifecycleContext) ThreadStash.get(LifecycleContext.class));

        } else if (view.redirect() != null) {
            processOutgoingFlashVars(view, requestCtx);

            HttpServletRequest request = (HttpServletRequest) requestCtx.getRequest();
            ((HttpServletResponse) requestCtx.getResponse()).sendRedirect(requests.toRequestURL(view.redirect(), request.isSecure(), request));
        }
        // Assuming stream.
        else {
            streamViewHandler.handle(view, requestCtx);
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
                request.setAttribute(flashVar.getKey(), flashVar.getValue());
            }
        }
    }

    protected void processOutgoingFlashVars(View view, RequestContext requestCtx) {
        Map<String, Object> flashVars = view.flashMap();

        if (flashVars == null || flashVars.size() == 0)
            return;

        HttpSession session = requestCtx.getSession(true);
        session.setAttribute(GeemvcKey.FLASH_VARS, flashVars);
    }

    protected String viewPath(String path) {
        return path.startsWith("/WEB-INF/") ? path : new StringBuilder("/WEB-INF").append(configuration.viewPrefix()).append(Char.SLASH).append(path).append(configuration.viewSuffix()).toString();
    }
}
