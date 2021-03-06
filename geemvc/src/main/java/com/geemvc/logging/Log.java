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

package com.geemvc.logging;

import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Created by Michael on 15.07.2016.
 */
public interface Log extends Logger {
    Log get(Class<?> declaringClass);

    void trace(String message, Supplier<?>... paramSuppliers);

    void debug(String message, Supplier<?>... paramSuppliers);

    void info(String message, Supplier<?>... paramSuppliers);

    void warn(String message, Supplier<?>... paramSuppliers);

    void error(String message, Supplier<?>... paramSuppliers);
}
