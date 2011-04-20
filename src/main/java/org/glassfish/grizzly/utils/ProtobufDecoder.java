package org.glassfish.grizzly.utils;

import org.glassfish.grizzly.*;
import org.glassfish.grizzly.attributes.AttributeStorage;
import org.glassfish.grizzly.attributes.Attribute;
import com.google.protobuf.Message;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Protocol Buffer Decoder
 *
 * @author Chad Dollins
 */
public class ProtobufDecoder extends AbstractTransformer<Buffer,Message> {
    public static final int INVALID_PROTOCOL_BUFFER_ERROR = 0;

    private static final Logger logger = Grizzly.logger(ProtobufDecoder.class);
    private static final int LENGTH_HEADER_SIZE = 4;

    private final Attribute<Integer> lengthAttribute;

    private final Message message;

    public ProtobufDecoder(final Message message) {
        this.message = message;

        lengthAttribute = attributeBuilder.createAttribute(
                "ProtobufDecoder.ProtoSize");
    }

    @Override
    protected TransformationResult<Buffer, Message> transformImpl(final AttributeStorage storage, final Buffer input)
            throws TransformationException {
        Integer protobufSize = lengthAttribute.get(storage);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "StringDecoder decode protobufSize={0} buffer={1} content={2}",
                    new Object[]{protobufSize, input, input.toStringContent()});
        }

        if (protobufSize == null) {
            if (input.remaining() < LENGTH_HEADER_SIZE) {
                return TransformationResult.createIncompletedResult(input);
            }

            protobufSize = input.getInt();
            lengthAttribute.set(storage, protobufSize);
        }

        if (input.remaining() < protobufSize) {
            return TransformationResult.createIncompletedResult(input);
        }

        final byte [] body = new byte[protobufSize];
        input.get(body);

        final Message protobufMessage;
        try {
            protobufMessage = message.newBuilderForType().mergeFrom(body).build();
        } catch (InvalidProtocolBufferException e) {
            final String msg = "InvalidProtocolBufferException during ProtocolBuffer construction.";
            logger.log(Level.WARNING, msg);
            return TransformationResult.createErrorResult(INVALID_PROTOCOL_BUFFER_ERROR, msg);
        }

        return TransformationResult.createCompletedResult(
                protobufMessage, input);
    }

    @Override
    public void release(final AttributeStorage storage) {
        lengthAttribute.remove(storage);
        super.release(storage);
    }

    @Override
    public String getName() {
        return ProtobufDecoder.class.getName();
    }

    @Override
    public boolean hasInputRemaining(final AttributeStorage storage, final Buffer input) {
        return input != null && input.hasRemaining();
    }
}
