/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.grizzly.utils;

import org.glassfish.grizzly.*;
import org.glassfish.grizzly.attributes.AttributeStorage;
import org.glassfish.grizzly.attributes.Attribute;
import com.google.protobuf.Message;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

/**
 * Protocol Buffer Decoder
 *
 * @author Chad Dollins
 */
public class ProtobufDecoder extends AbstractTransformer<Buffer,Message> {
    public static final int INVALID_PROTOCOL_BUFFER_ERROR = 0;
    public static final int IO_EXCEPTION_DURING_PROTOBUF_CREATION_ERROR = 1;

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

        final int pos = input.position();
        final BufferInputStream stream = new BufferInputStream(input, pos, pos + protobufSize);
        final Message protobufMessage;
        try {
            protobufMessage = message.newBuilderForType().mergeFrom(stream).build();
        } catch (InvalidProtocolBufferException e) {
            final String msg = "InvalidProtocolBufferException during ProtocolBuffer construction.";
            logger.log(Level.WARNING, msg);
            return TransformationResult.createErrorResult(INVALID_PROTOCOL_BUFFER_ERROR, msg);
        } catch (IOException e) {
            final String msg = "IOException during ProtocolBuffer construction.";
            logger.log(Level.WARNING, msg);
            return TransformationResult.createErrorResult(IO_EXCEPTION_DURING_PROTOBUF_CREATION_ERROR, msg);
        }

        return TransformationResult.createCompletedResult(protobufMessage, input);
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
