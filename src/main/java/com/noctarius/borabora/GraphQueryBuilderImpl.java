/*
 * Copyright (c) 2016, Christoph Engelbert (aka noctarius) and
 * contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.noctarius.borabora;

import com.noctarius.borabora.builder.GraphQueryBuilder;
import com.noctarius.borabora.builder.StreamGraphQueryBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static com.noctarius.borabora.DictionaryGraphQuery.floatMatcher;
import static com.noctarius.borabora.DictionaryGraphQuery.intMatcher;
import static com.noctarius.borabora.DictionaryGraphQuery.predicateMatcher;
import static com.noctarius.borabora.DictionaryGraphQuery.stringMatcher;

final class GraphQueryBuilderImpl
        implements StreamGraphQueryBuilder {

    private List<GraphQuery> graphQueries = new ArrayList<>();

    @Override
    public GraphQueryBuilder stream(long offset) {
        graphQueries.add(new StreamGraphQuery(offset));
        return this;
    }

    @Override
    public GraphQueryBuilder sequence(long index) {
        graphQueries.add(new SequenceGraphQuery(index));
        return this;
    }

    @Override
    public GraphQueryBuilder dictionary(Predicate<Value> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        graphQueries.add(predicateMatcher(predicate));
        return this;
    }

    @Override
    public GraphQueryBuilder dictionary(String key) {
        Objects.requireNonNull(key, "key must not be null");
        graphQueries.add(stringMatcher(key));
        return this;
    }

    @Override
    public GraphQueryBuilder dictionary(double key) {
        graphQueries.add(floatMatcher(key));
        return this;
    }

    @Override
    public GraphQueryBuilder dictionary(long key) {
        graphQueries.add(intMatcher(key));
        return this;
    }

    @Override
    public GraphQueryBuilder nullOrType(TypeSpec typeSpec) {
        graphQueries.add(new TypeMatcherGraphQuery(typeSpec, false));
        return this;
    }

    @Override
    public GraphQueryBuilder requireType(TypeSpec typeSpec) {
        graphQueries.add(new TypeMatcherGraphQuery(typeSpec, true));
        return this;
    }

    @Override
    public GraphQuery build() {
        if (graphQueries.size() == 0) {
            graphQueries.add(new StreamGraphQuery(-1));
        } else if (!(graphQueries.get(0) instanceof StreamGraphQuery)) {
            graphQueries.add(0, new StreamGraphQuery(-1));
        }
        return new ChainGraphQuery(graphQueries);
    }

}