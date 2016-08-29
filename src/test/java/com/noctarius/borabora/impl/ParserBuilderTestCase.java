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
import com.noctarius.borabora.Parser;
import com.noctarius.borabora.ValueType;
import com.noctarius.borabora.builder.ParserBuilder;
import com.noctarius.borabora.spi.TypeSpec;
import com.noctarius.borabora.spi.codec.CommonTagCodec;
import com.noctarius.borabora.spi.codec.TagDecoder;
import com.noctarius.borabora.spi.pipeline.PipelineStage;
import com.noctarius.borabora.spi.pipeline.PipelineStageFactory;
import com.noctarius.borabora.spi.pipeline.QueryBuilderNode;
import com.noctarius.borabora.spi.pipeline.QueryOptimizer;
import com.noctarius.borabora.spi.pipeline.QueryOptimizerStrategy;
import com.noctarius.borabora.spi.pipeline.QueryOptimizerStrategyFactory;
import com.noctarius.borabora.spi.pipeline.QueryPipeline;
import com.noctarius.borabora.spi.pipeline.QueryPipelineFactory;
import com.noctarius.borabora.spi.pipeline.QueryStage;
import com.noctarius.borabora.spi.query.BinarySelectStatementStrategy;
import com.noctarius.borabora.spi.query.ObjectSelectStatementStrategy;
import com.noctarius.borabora.spi.query.QueryConsumer;
import com.noctarius.borabora.spi.query.QueryContext;
import com.noctarius.borabora.spi.query.QueryContextFactory;
import com.noctarius.borabora.spi.query.SelectStatementStrategy;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class ParserBuilderTestCase {

    private static final QueryOptimizer QO_1 = new QueryOptimizerTestImpl();
    private static final QueryOptimizer QO_2 = new QueryOptimizerTestImpl();
    private static final QueryOptimizer QO_3 = new QueryOptimizerTestImpl();
    private static final QueryOptimizer QO_4 = new QueryOptimizerTestImpl();

    private static final TagDecoder TD_1 = new TagDecoderTestImpl();
    private static final TagDecoder TD_2 = new TagDecoderTestImpl();
    private static final TagDecoder TD_3 = new TagDecoderTestImpl();
    private static final TagDecoder TD_4 = new TagDecoderTestImpl();

    private static final PipelineStageFactory PIPELINE_STAGE_FACTORY = new PipelineStageFactoryTestImpl();
    private static final QueryContextFactory QUERY_CONTEXT_FACTORY = new QueryContextFactoryTestImpl();
    private static final QueryPipelineFactory QUERY_PIPELINE_FACTORY = new QueryPipelineFactoryTestImpl();
    private static final QueryOptimizerStrategyFactory QUERY_OPTIMIZER_STRATEGY_FACTORY = //
            new QueryOptimizerStrategyFactoryTestImpl();

    @Test
    public void test_withpipelinestagefactory() {
        ParserBuilder parserBuilder = new ParserBuilderImpl();
        parserBuilder.withPipelineStageFactory(PIPELINE_STAGE_FACTORY);
        Parser parser = parserBuilder.build();
        assertEquals(PIPELINE_STAGE_FACTORY, extractPipelineStageFactory(parser));
    }

    @Test
    public void test_withquerycontextfactory() {
        ParserBuilder parserBuilder = new ParserBuilderImpl();
        parserBuilder.withQueryContextFactory(QUERY_CONTEXT_FACTORY);
        Parser parser = parserBuilder.build();
        assertEquals(QUERY_CONTEXT_FACTORY, extractQueryContextFactory(parser));
    }

    @Test
    public void test_withqueryoptimizerstrategyfactory() {
        ParserBuilder parserBuilder = new ParserBuilderImpl();
        parserBuilder.withQueryOptimizerStrategyFactory(QUERY_OPTIMIZER_STRATEGY_FACTORY);
        Parser parser = parserBuilder.build();
        assertEquals(QUERY_OPTIMIZER_STRATEGY_FACTORY, extractQueryOptimizerStrategyFactory(parser));
    }

    @Test
    public void test_withquerypipelinefactory() {
        ParserBuilder parserBuilder = new ParserBuilderImpl();
        parserBuilder.withQueryPipelineFactory(QUERY_PIPELINE_FACTORY);
        Parser parser = parserBuilder.build();
        assertEquals(QUERY_PIPELINE_FACTORY, extractQueryPipelineFactory(parser));
    }

    @Test
    public void test_addqueryoptimizer_single() {
        ParserBuilder parserBuilder = new ParserBuilderImpl();
        parserBuilder.addQueryOptimizer(QO_1);
        Parser parser = parserBuilder.build();
        List<QueryOptimizer> queryOptimizers = extractQueryOptimizers(parser);
        assertEquals(1, queryOptimizers.size());
        assertEquals(QO_1, queryOptimizers.iterator().next());
    }

    @Test
    public void test_addqueryoptimizer_prevent_double_registration() {
        ParserBuilder parserBuilder = new ParserBuilderImpl();
        parserBuilder.addQueryOptimizer(QO_1);
        parserBuilder.addQueryOptimizer(QO_1);
        Parser parser = parserBuilder.build();
        List<QueryOptimizer> queryOptimizers = extractQueryOptimizers(parser);
        assertEquals(1, queryOptimizers.size());
        assertEquals(QO_1, queryOptimizers.iterator().next());
    }

    @Test
    public void test_addqueryoptimizer_double() {
        ParserBuilder parserBuilder = new ParserBuilderImpl();
        parserBuilder.addQueryOptimizers(QO_1, QO_2);
        Parser parser = parserBuilder.build();
        List<QueryOptimizer> queryOptimizers = extractQueryOptimizers(parser);
        assertEquals(2, queryOptimizers.size());
        Iterator<QueryOptimizer> iterator = queryOptimizers.iterator();
        assertEquals(QO_1, iterator.next());
        assertEquals(QO_2, iterator.next());
    }

    @Test
    public void test_addqueryoptimizer_array() {
        ParserBuilder parserBuilder = new ParserBuilderImpl();
        parserBuilder.addQueryOptimizers(QO_1, QO_2, QO_3, QO_4);
        Parser parser = parserBuilder.build();
        List<QueryOptimizer> queryOptimizers = extractQueryOptimizers(parser);
        assertEquals(4, queryOptimizers.size());
        Iterator<QueryOptimizer> iterator = queryOptimizers.iterator();
        assertEquals(QO_1, iterator.next());
        assertEquals(QO_2, iterator.next());
        assertEquals(QO_3, iterator.next());
        assertEquals(QO_4, iterator.next());
    }

    @Test
    public void test_addqueryoptimizer_iterable() {
        ParserBuilder parserBuilder = new ParserBuilderImpl();
        parserBuilder.addQueryOptimizers(Stream.of(QO_1, QO_2, QO_3, QO_4).collect(Collectors.toList()));
        Parser parser = parserBuilder.build();
        List<QueryOptimizer> queryOptimizers = extractQueryOptimizers(parser);
        assertEquals(4, queryOptimizers.size());
        Iterator<QueryOptimizer> iterator = queryOptimizers.iterator();
        assertEquals(QO_1, iterator.next());
        assertEquals(QO_2, iterator.next());
        assertEquals(QO_3, iterator.next());
        assertEquals(QO_4, iterator.next());
    }

    @Test
    public void test_addtagdecoder_single() {
        ParserBuilder parserBuilder = new ParserBuilderImpl();
        parserBuilder.addTagDecoder(TD_1);
        Parser parser = parserBuilder.build();
        List<TagDecoder> tagDecoders = extractTagDecoders(parser);
        assertEquals(2, tagDecoders.size());
        Iterator<TagDecoder> iterator = tagDecoders.iterator();
        assertEquals(CommonTagCodec.INSTANCE, iterator.next());
        assertEquals(TD_1, iterator.next());
    }

    @Test
    public void test_addtagdecoder_prevent_double_registration() {
        ParserBuilder parserBuilder = new ParserBuilderImpl();
        parserBuilder.addTagDecoder(TD_1);
        parserBuilder.addTagDecoder(TD_1);
        Parser parser = parserBuilder.build();
        List<TagDecoder> tagDecoders = extractTagDecoders(parser);
        assertEquals(2, tagDecoders.size());
        Iterator<TagDecoder> iterator = tagDecoders.iterator();
        assertEquals(CommonTagCodec.INSTANCE, iterator.next());
        assertEquals(TD_1, iterator.next());
    }

    @Test
    public void test_addtagdecoder_double() {
        ParserBuilder parserBuilder = new ParserBuilderImpl();
        parserBuilder.addTagDecoders(TD_1, TD_2);
        Parser parser = parserBuilder.build();
        List<TagDecoder> tagDecoders = extractTagDecoders(parser);
        assertEquals(3, tagDecoders.size());
        Iterator<TagDecoder> iterator = tagDecoders.iterator();
        assertEquals(CommonTagCodec.INSTANCE, iterator.next());
        assertEquals(TD_1, iterator.next());
        assertEquals(TD_2, iterator.next());
    }

    @Test
    public void test_addtagdecoder_array() {
        ParserBuilder parserBuilder = new ParserBuilderImpl();
        parserBuilder.addTagDecoders(TD_1, TD_2, TD_3, TD_4);
        Parser parser = parserBuilder.build();
        List<TagDecoder> tagDecoders = extractTagDecoders(parser);
        assertEquals(5, tagDecoders.size());
        Iterator<TagDecoder> iterator = tagDecoders.iterator();
        assertEquals(CommonTagCodec.INSTANCE, iterator.next());
        assertEquals(TD_1, iterator.next());
        assertEquals(TD_2, iterator.next());
        assertEquals(TD_3, iterator.next());
        assertEquals(TD_4, iterator.next());
    }

    @Test
    public void test_addtagdecoder_iterable() {
        ParserBuilder parserBuilder = new ParserBuilderImpl();
        parserBuilder.addTagDecoders(Stream.of(TD_1, TD_2, TD_3, TD_4).collect(Collectors.toList()));
        Parser parser = parserBuilder.build();
        List<TagDecoder> tagDecoders = extractTagDecoders(parser);
        assertEquals(5, tagDecoders.size());
        Iterator<TagDecoder> iterator = tagDecoders.iterator();
        assertEquals(CommonTagCodec.INSTANCE, iterator.next());
        assertEquals(TD_1, iterator.next());
        assertEquals(TD_2, iterator.next());
        assertEquals(TD_3, iterator.next());
        assertEquals(TD_4, iterator.next());
    }

    @Test
    public void test_asobjectselector() {
        ParserBuilder parserBuilder = new ParserBuilderImpl();
        parserBuilder.asObjectSelectStatementStrategy();
        Parser parser = parserBuilder.build();
        assertEquals(ObjectSelectStatementStrategy.INSTANCE, extractSelectStatementStrategy(parser));
    }

    @Test
    public void test_asbinaryselector() {
        ParserBuilder parserBuilder = new ParserBuilderImpl();
        parserBuilder.asObjectSelectStatementStrategy().asBinarySelectStatementStrategy();
        Parser parser = parserBuilder.build();
        assertEquals(BinarySelectStatementStrategy.INSTANCE, extractSelectStatementStrategy(parser));
    }

    @Test
    public void test_with_selectstrategy() {
        ParserBuilder parserBuilder = new ParserBuilderImpl();
        parserBuilder.withSelectStatementStrategy(ObjectSelectStatementStrategy.INSTANCE);
        Parser parser = parserBuilder.build();
        assertEquals(ObjectSelectStatementStrategy.INSTANCE, extractSelectStatementStrategy(parser));
    }

    private PipelineStageFactory extractPipelineStageFactory(Parser parser) {
        try {
            Field field = ParserImpl.class.getDeclaredField("pipelineStageFactory");
            field.setAccessible(true);
            return (PipelineStageFactory) field.get(parser);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private QueryOptimizerStrategyFactory extractQueryOptimizerStrategyFactory(Parser parser) {
        try {
            Field field = ParserImpl.class.getDeclaredField("queryOptimizerStrategyFactory");
            field.setAccessible(true);
            return (QueryOptimizerStrategyFactory) field.get(parser);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private QueryPipelineFactory extractQueryPipelineFactory(Parser parser) {
        try {
            Field field = ParserImpl.class.getDeclaredField("queryPipelineFactory");
            field.setAccessible(true);
            return (QueryPipelineFactory) field.get(parser);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private QueryContextFactory extractQueryContextFactory(Parser parser) {
        try {
            Field field = ParserImpl.class.getDeclaredField("queryContextFactory");
            field.setAccessible(true);
            return (QueryContextFactory) field.get(parser);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SelectStatementStrategy extractSelectStatementStrategy(Parser parser) {
        try {
            Field field = ParserImpl.class.getDeclaredField("selectStatementStrategy");
            field.setAccessible(true);
            return (SelectStatementStrategy) field.get(parser);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<QueryOptimizer> extractQueryOptimizers(Parser parser) {
        try {
            Field field = ParserImpl.class.getDeclaredField("queryOptimizers");
            field.setAccessible(true);
            return (List<QueryOptimizer>) field.get(parser);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<TagDecoder> extractTagDecoders(Parser parser) {
        try {
            Field field = ParserImpl.class.getDeclaredField("tagDecoders");
            field.setAccessible(true);
            return (List<TagDecoder>) field.get(parser);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class QueryOptimizerTestImpl
            implements QueryOptimizer {

        @Override
        public boolean handles(PipelineStage rooPipelineStage) {
            return false;
        }

        @Override
        public PipelineStage optimize(PipelineStage rootPipelineStage, PipelineStageFactory pipelineStageFactory) {
            return null;
        }
    }

    private static class TagDecoderTestImpl
            implements TagDecoder {

        @Override
        public Object process(ValueType valueType, long offset, long length, QueryContext queryContext) {
            return null;
        }

        @Override
        public boolean handles(Input input, long offset) {
            return false;
        }

        @Override
        public TypeSpec handles(long tagId) {
            return null;
        }

        @Override
        public ValueType valueType(Input input, long offset) {
            return null;
        }
    }

    private static class PipelineStageFactoryTestImpl
            implements PipelineStageFactory {

        @Override
        public PipelineStage newPipelineStage(PipelineStage left, PipelineStage right, QueryStage stage) {
            return null;
        }
    }

    private static class QueryContextFactoryTestImpl
            implements QueryContextFactory {

        @Override
        public QueryContext newQueryContext(Input input, QueryConsumer queryConsumer, List<TagDecoder> tagDecoders,
                                            SelectStatementStrategy selectStatementStrategy) {
            return null;
        }
    }

    private static class QueryOptimizerStrategyFactoryTestImpl
            implements QueryOptimizerStrategyFactory {

        @Override
        public QueryOptimizerStrategy newQueryOptimizerStrategy(List<QueryOptimizer> queryOptimizers) {
            return null;
        }
    }

    private static class QueryPipelineFactoryTestImpl
            implements QueryPipelineFactory {

        @Override
        public QueryPipeline newQueryPipeline(QueryBuilderNode queryRootNode, PipelineStageFactory pipelineStageFactory,
                                              QueryOptimizerStrategy queryOptimizerStrategy) {
            return null;
        }
    }

}