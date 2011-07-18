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

import junit.framework.TestCase;
import org.glassfish.grizzly.filterchain.*;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.*;

/**
 * Copyright Apr 20, 2011 Trustwave Holdings Inc. All Rights Reserved.
 * <p/>
 * $Id$
 */
public class ProtobufFilterTest extends TestCase {
    private static final Logger logger = Grizzly.logger(ProtobufFilterTest.class);
    private static final int PORT = 10011;

    @SuppressWarnings("unchecked")
    public void testSimpleMessageTest() throws IOException, ExecutionException, TimeoutException, InterruptedException {
        final Message.Person person = Message.Person.newBuilder().setAge(28).setName("vipaca").build();
        final FilterChainBuilder serverFilterChainBuilder = FilterChainBuilder.stateless();
        serverFilterChainBuilder.add(new TransportFilter())
                .add(new ProtobufFilter(person.getDefaultInstanceForType()))
                .add(new SimpleMessageTestFilter(person));

        final TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(serverFilterChainBuilder.build());

        Connection connection = null;
        try {
            transport.bind(PORT);
            transport.start();

            final Future<Connection> future = transport.connect("localhost", PORT);
            connection = future.get(10, TimeUnit.SECONDS);

            final BlockingQueue<Message.Person> resultQueue = DataStructures.getLTQInstance(Message.Person.class);

            final FilterChainBuilder clientFilterChainBuilder = FilterChainBuilder.stateless();
            clientFilterChainBuilder.add(new TransportFilter())
                    .add(new ProtobufFilter(person.getDefaultInstanceForType()))
                    .add(new ResultFilter(resultQueue));

            final FilterChain clientFilterChain = clientFilterChainBuilder.build();
            connection.setProcessor(clientFilterChain);

            final Future<WriteResult> writeResultFuture = connection.write(person);
            writeResultFuture.get(10, TimeUnit.SECONDS);

            final Message.Person peep = resultQueue.poll(10, TimeUnit.SECONDS);

            assertEquals(person, peep);
        }
        finally {
            if (connection != null) {
                connection.close();
            }

            transport.stop();
        }
    }

    private class SimpleMessageTestFilter extends BaseFilter {
        private final Message.Person person;

        public SimpleMessageTestFilter(final Message.Person person) {
            this.person = person;
        }

        @Override
        public NextAction handleRead(final FilterChainContext ctx) throws IOException {
            final Message.Person person = (Message.Person) ctx.getMessage();

            logger.log(Level.FINE, "Got the protobuf: {0}", person);

            assertEquals(this.person.getAge(), person.getAge());
            assertEquals(this.person.getName(), person.getName());
            assertEquals(this.person, person);

            ctx.write(person);

            return ctx.getStopAction();
        }
    }

    private class ResultFilter extends BaseFilter {
        private final BlockingQueue<Message.Person> resultQueue;


        public ResultFilter(final BlockingQueue<Message.Person> resultQueue) {

            this.resultQueue = resultQueue;
        }
        
        @Override
         public NextAction handleRead(final FilterChainContext ctx) throws IOException {
            resultQueue.add((Message.Person) ctx.getMessage());
            return ctx.getStopAction();
        }
    }
}

