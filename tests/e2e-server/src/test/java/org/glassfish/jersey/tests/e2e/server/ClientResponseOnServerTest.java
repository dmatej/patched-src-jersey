/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]

package org.glassfish.jersey.tests.e2e.server;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.Uri;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Test for support of client-side response in the server-side resource implementation.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ClientResponseOnServerTest extends JerseyTest {

    @Path("root")
    public static class RootResource {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Path("response")
        public Response getResponse(@Uri("internal/response") WebTarget target) {
            // returns client-side response instance
            return target.request(MediaType.TEXT_PLAIN).get();
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Path("error")
        public String getError(@Uri("internal/error") WebTarget target) {
            // throws WebApplicationException with an error response
            return target.request(MediaType.TEXT_PLAIN).get(String.class);
        }
    }

    @Path("internal")
    public static class InternalResource {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Path("response")
        public String getResponse() {
            return "response";
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Path("error")
        public Response getError() {
            // Testing for a cross-stack support of a completely custom status code.
            return Response.status(699).type(MediaType.TEXT_PLAIN).entity("error").build();
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(
                RootResource.class,
                InternalResource.class
        );
    }

    @Test
    public void testClientResponseUsageOnServer() {
        final WebTarget target = target("root/{type}");

        Response response;

        response = target.resolveTemplate("type", "response").request(MediaType.TEXT_PLAIN).get();
        assertEquals(200, response.getStatus());
        assertEquals("response", response.readEntity(String.class));

        response = target.resolveTemplate("type", "error").request(MediaType.TEXT_PLAIN).get();
        assertEquals(699, response.getStatus());
        assertEquals("error", response.readEntity(String.class));
    }
}