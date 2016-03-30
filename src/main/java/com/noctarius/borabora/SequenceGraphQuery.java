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

import java.util.Collection;

final class SequenceGraphQuery
        implements GraphQuery {

    private final long sequenceIndex;

    SequenceGraphQuery(long sequenceIndex) {
        this.sequenceIndex = sequenceIndex;
    }

    @Override
    public long access(Decoder stream, long offset, Collection<SemanticTagProcessor> processors) {
        short head = stream.transientUint8(offset);
        MajorType majorType = MajorType.findMajorType(head);

        // Stream direct access (return actual object itself)
        if (sequenceIndex == -1) {
            return offset;
        }

        if (MajorType.Sequence != majorType) {
            throw new WrongTypeException("Not a sequence");
        }

        // Sequences need head skipped
        long elementCount = majorType.elementCount(stream, offset);
        if (elementCount < sequenceIndex) {
            return -1;
        }

        // Element access
        long headByteSize = ByteSizes.headByteSize(stream, offset);
        offset += headByteSize;

        // Stream objects
        return skip(stream, offset);
    }

    private long skip(Decoder stream, long offset) {
        // Skip unnecessary objects
        for (int i = 0; i < sequenceIndex; i++) {
            offset = stream.skip(offset);
        }
        return offset;
    }

}
