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

package com.geemvc.taglib.html;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.jsp.JspException;

import org.apache.commons.lang3.StringEscapeUtils;

import com.geemvc.Char;
import com.geemvc.Str;
import com.geemvc.i18n.message.CompositeMessageResolver;
import com.geemvc.taglib.HtmlTagSupport;

public class MessageTagSupport extends HtmlTagSupport {
    protected Object key;

    protected Locale locale;

    protected String lang;

    protected String country;

    protected final CompositeMessageResolver messageResolver;

    protected Pattern parameterPattern = Pattern.compile("p[\\d]+");

    public MessageTagSupport() {
        this.messageResolver = injector.getInstance(CompositeMessageResolver.class);
    }

    @Override
    public void doTag() throws JspException {
        if (locale != null && (lang != null || country != null))
            throw new JspException("You can only set one of of either 'locale' or a 'language/country' combination.");

        if (lang != null && country != null)
            locale = new Locale(lang, country);

        else if (lang != null)
            locale = new Locale(lang);

        String label = null;

        // Handle string keys normally.
        if (key instanceof String) {
            label = messageResolver.resolve((String) key, locale, requestContext(), true);
        } else if (key.getClass().isEnum()) {
            // Attempt to resolve <enun-fqn>.<enum-value>.
            label = messageResolver.resolve(new StringBuilder(key.getClass().getName()).append(Char.DOT).append(key).toString(), requestContext(), true);

            // Attempt to resolve <enun-simple-name>.<enum-value>.
            if (label == null)
                label = messageResolver.resolve(new StringBuilder(key.getClass().getSimpleName()).append(Char.DOT).append(key).toString(), requestContext(), true);
        } else if (key instanceof Boolean) {
            // Attempt to resolve Boolean.true or Boolean.false.
            label = messageResolver.resolve(new StringBuilder(Boolean.class.getSimpleName()).append(Char.DOT).append(String.valueOf(key).toLowerCase()).toString(), requestContext(), true);
        } else {
            throw new JspException("The type '" + key.getClass().getName() + "' cannot be used as a message key in MessageTagSupport. Only the types String, Boolean or enums are supported.");
        }

        if (label != null) {
            if (escapeHTML)
                label = StringEscapeUtils.escapeHtml4(label);

            if (escapeJavascript)
                label = StringEscapeUtils.escapeEcmaScript(label);

            if (escapeJson)
                label = StringEscapeUtils.escapeJson(label);

            if (unescapeHTML)
                label = StringEscapeUtils.unescapeHtml4(label);

            if (unescapeJavascript)
                label = StringEscapeUtils.unescapeEcmaScript(label);

            if (unescapeJson)
                label = StringEscapeUtils.unescapeJson(label);
        }

        if (label == null) {
            label = getBodyContent();

            if (label == null)
                label = String.format("???%s???", key);
        }

        // Deal with parameters.
        if (label != null) {
            List<Object> params = messageParameters();

            if (params != null && !params.isEmpty())
                label = MessageFormat.format(label, params.toArray());
        }

        if (!Str.isEmpty(var)) {
            jspContext.setAttribute(var, label, scope());
        } else {
            try {
                jspContext.getOut().write(label);
            } catch (IOException e) {
                throw new JspException(e);
            }
        }
    }

    protected List<Object> messageParameters() {
        List<Object> parameters = null;

        if (!dynamicAttributes.isEmpty()) {
            Map<Integer, Object> parameterMap = new LinkedHashMap<>();

            Set<Entry<String, Object>> entries = dynamicAttributes.entrySet();

            for (Entry<String, Object> entry : entries) {
                Matcher m = parameterPattern.matcher(entry.getKey());

                if (m.matches())
                    parameterMap.put(Integer.valueOf(entry.getKey().substring(1)) - 1, entry.getValue());
            }

            Map<Integer, Object> sortedParameters = parameterMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (e1, e2) -> e2, LinkedHashMap::new));

            parameters = new ArrayList<>(sortedParameters.values());
        }

        return parameters;
    }

    public Object getKey() {
        return key;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
