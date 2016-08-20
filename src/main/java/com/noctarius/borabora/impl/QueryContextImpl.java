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
package com.noctarius.borabora.impl;

import com.noctarius.borabora.Input;
import com.noctarius.borabora.MajorType;
import com.noctarius.borabora.Value;
import com.noctarius.borabora.ValueType;
import com.noctarius.borabora.spi.codec.Decoder;
import com.noctarius.borabora.spi.codec.TagDecoder;
import com.noctarius.borabora.spi.query.QueryConsumer;
import com.noctarius.borabora.spi.query.QueryContext;
import com.noctarius.borabora.spi.query.QueryContextFactory;
import com.noctarius.borabora.spi.query.SelectStatementStrategy;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

final class QueryContextImpl
        implements QueryContext {

    private final List<TagDecoder> tagDecoders;
    private final QueryConsumer queryConsumer;
    private final QueryContextFactory queryContextFactory;
    private final SelectStatementStrategy selectStatementStrategy;
    private final Input input;

    // Queries are inherently thread-safe!
    private Deque<Object> stack;
    private long offset;

    QueryContextImpl(Input input, QueryConsumer queryConsumer, List<TagDecoder> tagDecoders,
                     SelectStatementStrategy selectStatementStrategy, QueryContextFactory queryContextFactory) {

        this.input = input;
        this.queryConsumer = queryConsumer;
        this.tagDecoders = tagDecoders;
        this.selectStatementStrategy = selectStatementStrategy;
        this.queryContextFactory = queryContextFactory;
    }

    @Override
    public Input input() {
        return input;
    }

    @Override
    public long offset() {
        return offset;
    }

    @Override
    public void offset(long offset) {
        this.offset = offset;
    }

    @Override
    public List<TagDecoder> tagDecoders() {
        return tagDecoders;
    }

    @Override
    public SelectStatementStrategy selectStatementStrategy() {
        return selectStatementStrategy;
    }

    @Override
    public QueryContextFactory queryContextFactory() {
        return queryContextFactory;
    }

    @Override
    public void consume(long offset) {
        queryConsumer.accept(offset, this);
    }

    @Override
    public void consume(Value value) {
        queryConsumer.consume(value);
    }

    @Override
    public <T> T applyDecoder(long offset, MajorType majorType, ValueType valueType) {
        TagDecoder<T> processor = findProcessor(offset);
        if (processor == null) {
            return null;
        }
        long length = Decoder.length(input, majorType, offset);
        return processor.process(valueType, offset, length, this);
    }

    @Override
    public <T> void queryStackPush(T element) {
        getStack().addFirst(element);
    }

    @Override
    public <T> T queryStackPop() {
        return (T) getStack().removeFirst();
    }

    @Override
    public <T> T queryStackPeek() {
        return (T) getStack().peekFirst();
    }

    private <V> TagDecoder<V> findProcessor(long offset) {
        for (int i = 0; i < tagDecoders.size(); i++) {
            TagDecoder<V> tagDecoder = tagDecoders.get(i);
            if (tagDecoder.handles(input, offset)) {
                return tagDecoder;
            }
        }
        return null;
    }

    // Queries are inherently thread-safe!
    private Deque<Object> getStack() {
        if (stack == null) {
            stack = new LinkedList<>();
        }
        return stack;
    }

}
