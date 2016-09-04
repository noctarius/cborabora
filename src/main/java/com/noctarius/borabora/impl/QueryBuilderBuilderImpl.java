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

import com.noctarius.borabora.builder.QueryBuilderBuilder;
import com.noctarius.borabora.builder.query.StreamQueryBuilder;
import com.noctarius.borabora.impl.query.BTreeFactories;
import com.noctarius.borabora.spi.query.BinaryProjectionStrategy;
import com.noctarius.borabora.spi.query.ProjectionStrategy;
import com.noctarius.borabora.spi.query.pipeline.PipelineStageFactory;
import com.noctarius.borabora.spi.query.optimizer.QueryOptimizer;
import com.noctarius.borabora.spi.query.optimizer.QueryOptimizerStrategy;
import com.noctarius.borabora.spi.query.optimizer.QueryOptimizerStrategyFactory;
import com.noctarius.borabora.spi.query.pipeline.QueryPipelineFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class QueryBuilderBuilderImpl
        implements QueryBuilderBuilder {

    private final List<QueryOptimizer> queryOptimizers = new ArrayList<>();

    private ProjectionStrategy projectionStrategy = BinaryProjectionStrategy.INSTANCE;
    private PipelineStageFactory pipelineStageFactory = BTreeFactories.newPipelineStageFactory();
    private QueryPipelineFactory queryPipelineFactory = BTreeFactories.newQueryPipelineFactory();
    private QueryOptimizerStrategyFactory queryOptimizerStrategyFactory = BTreeFactories.newQueryOptimizerStrategyFactory();

    @Override
    public QueryBuilderBuilder withProjectionStrategy(ProjectionStrategy projectionStrategy) {
        Objects.requireNonNull(projectionStrategy, "projectionStrategy must not be null");
        this.projectionStrategy = projectionStrategy;
        return this;
    }

    @Override
    public QueryBuilderBuilder withQueryPipelineFactory(QueryPipelineFactory queryPipelineFactory) {
        Objects.requireNonNull(queryPipelineFactory, "queryPipelineFactory must not be null");
        this.queryPipelineFactory = queryPipelineFactory;
        return this;
    }

    @Override
    public QueryBuilderBuilder withPipelineStageFactory(PipelineStageFactory pipelineStageFactory) {
        Objects.requireNonNull(pipelineStageFactory, "pipelineStageFactory must not be null");
        this.pipelineStageFactory = pipelineStageFactory;
        return this;
    }

    @Override
    public QueryBuilderBuilder withQueryOptimizerStrategyFactory(QueryOptimizerStrategyFactory queryOptimizerStrategyFactory) {
        Objects.requireNonNull(queryOptimizerStrategyFactory, "queryOptimizerStrategyFactory must not be null");
        this.queryOptimizerStrategyFactory = queryOptimizerStrategyFactory;
        return this;
    }

    @Override
    public QueryBuilderBuilder addQueryOptimizer(QueryOptimizer queryOptimizer) {
        Objects.requireNonNull(queryOptimizer, "queryOptimizer must not be null");
        if (!this.queryOptimizers.contains(queryOptimizer)) {
            this.queryOptimizers.add(queryOptimizer);
        }
        return this;
    }

    @Override
    public QueryBuilderBuilder addQueryOptimizers(QueryOptimizer queryOptimizer1, QueryOptimizer queryOptimizer2) {
        addQueryOptimizer(queryOptimizer1);
        addQueryOptimizer(queryOptimizer2);
        return this;
    }

    @Override
    public QueryBuilderBuilder addQueryOptimizers(QueryOptimizer queryOptimizer1, QueryOptimizer queryOptimizer2,
                                                  QueryOptimizer... queryOptimizers) {

        Objects.requireNonNull(queryOptimizers, "queryOptimizers must not be null");
        addQueryOptimizer(queryOptimizer1);
        addQueryOptimizer(queryOptimizer2);
        for (QueryOptimizer queryOptimizer : queryOptimizers) {
            addQueryOptimizer(queryOptimizer);
        }
        return this;
    }

    @Override
    public QueryBuilderBuilder addQueryOptimizers(Iterable<QueryOptimizer> queryOptimizers) {
        Objects.requireNonNull(queryOptimizers, "queryOptimizers must not be null");
        for (QueryOptimizer queryOptimizer : queryOptimizers) {
            addQueryOptimizer(queryOptimizer);
        }
        return this;
    }

    @Override
    public StreamQueryBuilder newBuilder() {
        QueryOptimizerStrategy queryOptimizerStrategy = queryOptimizerStrategyFactory.newQueryOptimizerStrategy(queryOptimizers);
        return new QueryBuilderImpl(projectionStrategy, queryOptimizerStrategy, pipelineStageFactory, queryPipelineFactory);
    }

}
