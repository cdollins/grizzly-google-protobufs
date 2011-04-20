package org.glassfish.grizzly.utils;

import org.glassfish.grizzly.*;
import org.glassfish.grizzly.attributes.AttributeStorage;
import com.google.protobuf.Message;

import java.util.logging.Logger;

/**
 * Protocol Buffer Encoder
 *
 * @author Chad Dollins
 */
public class ProtobufEncoder extends AbstractTransformer<Message,Buffer> {
    private static final Logger logger = Grizzly.logger(ProtobufEncoder.class);
    private static final int LENGTH_HEADER_SIZE = 4;

    @Override
    protected TransformationResult<Message, Buffer> transformImpl(final AttributeStorage storage, final Message input) throws TransformationException {
        if (input == null) {
            throw new TransformationException("Input could not be null");
        }

        final byte[] byteRepresentation = input.toByteArray();

        final Buffer output =
                obtainMemoryManager(storage).allocate(byteRepresentation.length + LENGTH_HEADER_SIZE);

        output.putInt(byteRepresentation.length);

        output.put(byteRepresentation);

        output.flip();
        output.allowBufferDispose(true);

        return TransformationResult.createCompletedResult(output, null);
    }

    @Override
    public String getName() {
        return ProtobufEncoder.class.getName();
    }

    @Override
    public boolean hasInputRemaining(final AttributeStorage storage, final Message input) {
        return input != null;
    }
}
