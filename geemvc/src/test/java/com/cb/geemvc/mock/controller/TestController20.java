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

package com.cb.geemvc.mock.controller;

import com.cb.geemvc.HttpMethod;
import com.cb.geemvc.Views;
import com.cb.geemvc.annotation.Controller;
import com.cb.geemvc.annotation.Request;
import com.cb.geemvc.bind.param.annotation.Data;
import com.cb.geemvc.bind.param.annotation.PathParam;
import com.cb.geemvc.mock.bean.Person;
import com.cb.geemvc.mock.repository.Persons;
import com.cb.geemvc.view.bean.View;
import com.google.inject.Inject;

@Controller
@Request("/persons")
public class TestController20 {
    protected final Persons persons;

    @Inject
    public TestController20(Persons persons) {
        this.persons = persons;
    }

    @Request(path = "/")
    public View getAll() {
        return Views.forward("person/list").bind("persons", persons.all());
    }

    @Request(path = "/{id}")
    public View get(@Data Person person) {
        return Views.forward("person/details").bind("person", person);
    }

    @Request(path = "/", method = HttpMethod.POST)
    public View create(Person person) {
        Person p = persons.add(person);

        return Views.redirect("/persons/success").bind("person", p);
    }

    @Request(path = "/{id}", method = HttpMethod.PUT)
    public String update(@PathParam("id") Long id, @Data Person person) {
        if (person != null && person.getId() != null)
            persons.update(person);

        return "redirect:/persons/details/" + id;
    }

    @Request(path = "/{id}", method = HttpMethod.DELETE)
    public String delete(@Data Person person) {
        if (person != null)
            persons.remove(person);

        return "redirect:/persons";
    }
}
