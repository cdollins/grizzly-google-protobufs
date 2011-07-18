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
    private static final byte [] LENGTH_HEADER_BUFFER = new byte[LENGTH_HEADER_SIZE];

    @Override
    protected TransformationResult<Message, Buffer> transformImpl(final AttributeStorage storage, final Message input) throws TransformationException {
        if (input == null) {
            throw new TransformationException("Input could not be null");
        }

        final MemoryManager mm = obtainMemoryManager(storage);
        final BufferOutputStream bos = new BufferOutputStream(mm);

        try {
            bos.write(LENGTH_HEADER_BUFFER);
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
