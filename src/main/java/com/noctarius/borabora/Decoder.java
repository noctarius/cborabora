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

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.function.Predicate;

import static com.noctarius.borabora.Constants.ADDITIONAL_INFORMATION_MASK;
import static com.noctarius.borabora.Constants.FP_VALUE_FALSE;
import static com.noctarius.borabora.Constants.FP_VALUE_NULL;
import static com.noctarius.borabora.Constants.FP_VALUE_TRUE;
import static com.noctarius.borabora.Constants.OPCODE_BREAK_MASK;

final class Decoder {

    private static final Charset ASCII = Charset.forName("ASCII");
    private static final Charset UTF8 = Charset.forName("UTF8");

    private final Input input;

    Decoder(Input input) {
        this.input = input;
    }

    short transientUint8(long index) {
        return readUint8(index);
    }

    short readUint8(long index) {
        byte v = readInt8(index);
        return (short) (v & 0xFF);
    }

    byte readInt8(long index) {
        return input.read(index);
    }

    short readInt16(long index) {
        short b1 = readUint8(index);
        short b2 = readUint8(index + 1);
        return (short) ((b1 << 8) | b2);
    }

    int readUint16(long index) {
        return readInt16(index) & 0xffff;
    }

    int readInt32(long index) {
        int b1 = readUint8(index);
        int b2 = readUint8(index + 1);
        int b3 = readUint8(index + 2);
        int b4 = readUint8(index + 3);
        return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
    }

    long readUint32(long index) {
        return readInt32(index) & 0xffffffff;
    }

    long readUint64(long index) {
        long b1 = readUint8(index);
        long b2 = readUint8(index + 1);
        long b3 = readUint8(index + 2);
        long b4 = readUint8(index + 3);
        long b5 = readUint8(index + 4);
        long b6 = readUint8(index + 5);
        long b7 = readUint8(index + 6);
        long b8 = readUint8(index + 7);
        return (b1 << 56) | (b2 << 48) | (b3 << 40) | (b4 << 32) | (b5 << 24) | (b6 << 16) | (b7 << 8) | b8;
    }

    BigInteger readUint64BigInt(long index) {
        byte b1 = readInt8(index);
        byte b2 = readInt8(index + 1);
        byte b3 = readInt8(index + 2);
        byte b4 = readInt8(index + 3);
        byte b5 = readInt8(index + 4);
        byte b6 = readInt8(index + 5);
        byte b7 = readInt8(index + 6);
        byte b8 = readInt8(index + 7);
        return new BigInteger(1, new byte[]{b1, b2, b3, b4, b5, b6, b7, b8});
    }

    Number readInt(long index) {
        short head = transientUint8(index);
        if (isNull(head)) {
            return null;
        }
        long mask = -((head & 0xff) >>> 5);
        int byteSize = ByteSizes.intByteSize(this, head);
        Number number;
        switch (byteSize) {
            case 2:
                number = mask ^ readUint8(index + 1);
                break;
            case 3:
                number = mask ^ readUint16(index + 1);
                break;
            case 5:
                number = mask ^ readUint32(index + 1);
                break;
            case 9:
                number = BigInteger.valueOf(mask).xor(readUint64BigInt(index + 1));
                break;
            default:
                number = mask ^ (head & ADDITIONAL_INFORMATION_MASK);
        }
        return number;
    }

    Number readUint(long index) {
        short head = transientUint8(index);
        if (isNull(head)) {
            return null;
        }
        int byteSize = ByteSizes.intByteSize(this, head);
        Number number;
        switch (byteSize) {
            case 2:
                number = readUint8(index + 1);
                break;
            case 3:
                number = readUint16(index + 1);
                break;
            case 5:
                number = readUint32(index + 1);
                break;
            case 9:
                number = readUint64BigInt(index + 1);
                break;
            default:
                number = head & ADDITIONAL_INFORMATION_MASK;
        }
        return number;
    }

    Number readFloat(long index) {
        short head = transientUint8(index);
        if (isNull(head)) {
            return null;
        }
        int addInfo = additionInfo(head);
        switch (addInfo) {
            case 25:
                return readHalfFloatValue(index + 1);
            case 26:
                return readSinglePrecisionFloat(index + 1);
            case 27:
                return readDoublePrecisionFloat(index + 1);
            default:
                throw new IllegalStateException("Additional Info '" + addInfo + "' is not a floating point value");
        }
    }

    Number readNumber(ValueType valueType, long index) {
        if (ValueTypes.Float.equals(valueType)) {
            return readFloat(index);
        }
        return readInt(index);
    }

    String readString(long index) {
        short head = transientUint8(index);
        if (isNull(head)) {
            return null;
        }
        int addInfo = head & ADDITIONAL_INFORMATION_MASK;
        if (addInfo == 31) {
            // Concatenated string!
            long position = index + 1;
            StringBuilder sb = new StringBuilder();
            while (true) {
                short h = transientUint8(position);
                if ((h & OPCODE_BREAK_MASK) == OPCODE_BREAK_MASK) {
                    break;
                }
                sb.append(readString(position));
                position += ByteSizes.textStringByteSize(this, position);
            }
            return sb.toString();
        }

        return readString0(index);
    }

    Sequence readSequence(long index, Collection<SemanticTagProcessor> processors) {
        short head = transientUint8(index);
        if (isNull(head)) {
            return null;
        }
        long headByteSize = ByteSizes.headByteSize(this, index);
        long size = ElementCounts.sequenceElementCount(this, index);
        long[][] elementIndexes = readElementIndexes(index + headByteSize, size);
        return new SequenceImpl(this, size, elementIndexes, processors);
    }

    Dictionary readDictionary(long index, Collection<SemanticTagProcessor> processors) {
        short head = transientUint8(index);
        if (isNull(head)) {
            return null;
        }
        long headByteSize = ByteSizes.headByteSize(this, index);
        long size = ElementCounts.dictionaryElementCount(this, index);
        long[][] elementIndexes = readElementIndexes(index + headByteSize, size * 2);
        return new DictionaryImpl(this, size, elementIndexes, processors);
    }

    long length(MajorType majorType, long index) {
        short head = transientUint8(index);
        switch (majorType) {
            case UnsignedInteger:
            case NegativeInteger:
                return ByteSizes.intByteSize(this, head);
            case ByteString:
            case TextString:
                return ByteSizes.stringByteSize(this, index);
            case Sequence:
                return ByteSizes.sequenceByteSize(this, index);
            case Dictionary:
                return ByteSizes.dictionaryByteSize(this, index);
            case SemanticTag:
                return ByteSizes.semanticTagByteSize(this, index);
            case FloatingPointOrSimple:
                return ByteSizes.floatingPointOrSimpleByteSize(this, index);
        }
        throw new IllegalStateException("Illegal MajorType requested");
    }

    long skip(long index) {
        short head = transientUint8(index);
        MajorType majorType = MajorType.findMajorType(head);
        return skip(majorType, index);
    }

    long skip(MajorType majorType, long index) {
        long size = length(majorType, index);
        return index + size;
    }

    boolean isNull(short head) {
        MajorType majorType = MajorType.findMajorType(head);
        if (MajorType.FloatingPointOrSimple != majorType) {
            return false;
        }
        int addInfo = head & ADDITIONAL_INFORMATION_MASK;
        return addInfo == FP_VALUE_NULL;
    }

    boolean getBooleanValue(long index) {
        short head = transientUint8(index);
        // Null is legal for all types
        MajorType majorType = MajorType.findMajorType(head);
        if (MajorType.FloatingPointOrSimple == majorType) {
            int addInfo = head & ADDITIONAL_INFORMATION_MASK;
            switch (addInfo) {
                case FP_VALUE_FALSE:
                    return false;
                case FP_VALUE_TRUE:
                    return true;
            }
        }
        throw new IllegalStateException("Illegal boolean value");
    }

    float readHalfFloatValue(long index) {
        int v = readUint16(index);

        // based on http://stackoverflow.com/questions/5678432/decompressing-half-precision-floats-in-javascript/5684578#5684578
        int s = (v & 0x8000) >> 15;
        int e = (v & 0x7c00) >> 10;
        int f = v & 0x03ff;

        if (e == 0) {
            return (float) ((s == 0 ? 1 : -1) * Math.pow(2, -14) * (f / Math.pow(2, 10)));
        } else if (e == 0x1f) {
            return f != 0 ? Float.NaN : (s == 0 ? 1 : -1) * Float.POSITIVE_INFINITY;
        }
        return (float) ((s == 0 ? 1 : -1) * Math.pow(2, e - 15) * (1 + f / Math.pow(2, 10)));
    }

    float readSinglePrecisionFloat(long index) {
        return Float.intBitsToFloat((int) readUint32(index));
    }

    double readDoublePrecisionFloat(long index) {
        return Double.longBitsToDouble(readUint64(index));
    }

    long findByDictionaryKey(Predicate<Value> predicate, long index, long count, Collection<SemanticTagProcessor> processors) {
        // Search for key element
        long position = findByPredicate(predicate, index, count, processors);
        if (position == -1) {
            return -1;
        }
        return skip(position);
    }

    long findDictionaryKey(Predicate<Value> predicate, long index, long count, Collection<SemanticTagProcessor> processors) {
        // Skip first key
        long position = skip(index);

        // Search for value element
        return findByPredicate(predicate, position, count, processors);
    }

    long findDictionaryValue(Predicate<Value> predicate, long index, long count, Collection<SemanticTagProcessor> processors) {
        // Skip first key
        long position = skip(index);

        // Search for value element
        return findByPredicate(predicate, position, count, processors);
    }

    StreamValue readValue(long index, Collection<SemanticTagProcessor> processors) {
        short head = transientUint8(index);
        MajorType mt = MajorType.findMajorType(head);
        ValueType vt = ValueTypes.valueType(this, index);
        long length = length(mt, index);
        return new StreamValue(mt, vt, this, index, length, processors);
    }

    int additionInfo(long index) {
        short head = transientUint8(index);
        return additionInfo(head);
    }

    int additionInfo(short head) {
        return head & ADDITIONAL_INFORMATION_MASK;
    }

    byte[] readRaw(long index, long length) {
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Extraction of huge data (> Integer.MAX_VALUE) is not supported");
        }

        byte[] data = new byte[(int) length];
        for (int i = 0; i < data.length; i++) {
            data[i] = readInt8(index + i);
        }
        return data;
    }

    private long findByPredicate(Predicate<Value> predicate, long index, long count,
                                 Collection<SemanticTagProcessor> processors) {

        for (int i = 0; i < count; i++) {
            short head = transientUint8(index);
            MajorType mt = MajorType.findMajorType(head);
            ValueTypes vt = ValueTypes.valueType(this, index);
            long l = length(mt, index);
            if (predicate.test(new StreamValue(mt, vt, this, index, l, processors))) {
                return index;
            }
            index = skip(index + l);
        }
        return -1;

    }

    private String readString0(long index) {
        short head = transientUint8(index);
        MajorType majorType = MajorType.findMajorType(head);
        long byteSize = ByteSizes.textStringByteSize(this, index);
        if (byteSize > Integer.MAX_VALUE) {
            throw new IllegalStateException("Strings of size > Integer.MAX_VALUE are not implemented");
        }
        int dataSize = (int) ByteSizes.stringDataSize(this, index);
        byte[] data = new byte[dataSize];
        for (int i = 0; i < dataSize; i++) {
            data[i] = readInt8(index + 1 + i);
        }
        if (MajorType.ByteString == majorType) {
            return new String(data, ASCII);
        }
        return new String(data, UTF8);
    }

    private long[][] readElementIndexes(long index, long elementSize) {
        int baseSize = (int) (elementSize / Integer.MAX_VALUE) + 1;
        long[][] elementIndexes = new long[baseSize][];

        long position = index;
        long remainingElements = elementSize;
        for (int base = 0; base < baseSize; base++) {
            int remain = (int) Math.min(Integer.MAX_VALUE, remainingElements);
            elementIndexes[base] = new long[remain];
            for (int elIndex = 0; elIndex < remain; elIndex++) {
                // Store element position
                elementIndexes[base][elIndex] = position;

                // Skip elements content to next element
                short head = transientUint8(position);
                MajorType majorType = MajorType.findMajorType(head);
                position += length(majorType, position);
            }
        }
        return elementIndexes;
    }

}