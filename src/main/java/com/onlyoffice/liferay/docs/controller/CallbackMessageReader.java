/**
 *
 * (c) Copyright Ascensio System SIA 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.onlyoffice.liferay.docs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.model.documenteditor.Callback;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

@Consumes(MediaType.APPLICATION_JSON)
public class CallbackMessageReader implements MessageBodyReader<Callback> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean isReadable(final Class<?> aClass, final Type type, final Annotation[] annotations,
                              final MediaType mediaType) {
        return true;
    }

    @Override
    public Callback readFrom(final Class<Callback> aClass, final Type type, final Annotation[] annotations,
                             final MediaType mediaType, final MultivaluedMap<String, String> multivaluedMap,
                             final InputStream inputStream)
            throws IOException, WebApplicationException {
        return objectMapper.readValue(inputStream, Callback.class);
    }
}
