package org.glassfish.grizzly.utils;

import org.glassfish.grizzly.*;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.attributes.AttributeStorage;
import com.google.protobuf.Message;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;

/**
 * Protocol Buffer Encoder
 *
 * @author Chad Dollins
 */
public class ProtobufEncoder extends AbstractTransformer<Message,Buffer> {
    public static final int IO_ERROR_FROM_BUFFER_OUTPUT_STREAM = 0;
    private static final Logger logger = Grizzly.logger(ProtobufEncoder.class);
    private static final int LENGTH_HEADER_SIZE = 4;

    @Override
    protected TransformationResult<Message, Buffer> transformImpl(final AttributeStorage storage, final Message input) throws TransformationException {
        if (input == null) {
            throw new TransformationException("Input could not be null");
        }

        final MemoryManager mm = obtainMemoryManager(storage);
        final BufferOutputStream bos = new BufferOutputStream(mm);

        try {
            bos.write(new byte[LENGTH_HEADER_SIZE]);
            input.writeTo(bos);
            bos.close();
        } catch (IOException e) {
            final String msg = "IOException during construction of BufferOutputStream from Protocol Buffer.";
            logger.log(Level.WARNING, msg);
            return TransformationResult.createErrorResult(IO_ERROR_FROM_BUFFER_OUTPUT_STREAM, msg);
        }


        final Buffer output = bos.getBuffer().flip();
        final int len = output.remaining() - LENGTH_HEADER_SIZE;
        output.putInt(0, len);
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
