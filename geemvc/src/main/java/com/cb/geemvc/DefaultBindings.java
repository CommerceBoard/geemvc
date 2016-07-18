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

package com.cb.geemvc;

import java.util.List;
import java.util.Map;

import com.cb.geemvc.validation.Errors;

public class DefaultBindings implements Bindings {
    protected Map<String, List<String>> requestValues;
    protected Map<String, Object> typedValues;
    protected Errors errors;

    @Override
    public Bindings build(Map<String, List<String>> requestValues, Map<String, Object> typedValues, Errors errors) {
        this.requestValues = requestValues;
        this.typedValues = typedValues;
        this.errors = errors;
        return this;
    }

    @Override
    public Map<String, List<String>> requestValues() {
        return requestValues;
    }

    @Override
    public Map<String, Object> typedValues() {
        return typedValues;
    }

    @Override
    public Errors errors() {
        return errors;
    }

    @Override
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
}
