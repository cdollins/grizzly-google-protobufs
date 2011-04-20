package org.glassfish.grizzly.utils;

import org.glassfish.grizzly.filterchain.AbstractCodecFilter;
import org.glassfish.grizzly.Transformer;
import org.glassfish.grizzly.Buffer;
import com.google.protobuf.Message;

/**
 * Copyright Apr 14, 2011 Trustwave Holdings Inc. All Rights Reserved.
 * <p/>
 * $Id$
 */
public class ProtobufFilter extends AbstractCodecFilter<Buffer, Message> {
    public ProtobufFilter(final Message message) {
        super(new ProtobufDecoder(message), new ProtobufEncoder());
    }
}
