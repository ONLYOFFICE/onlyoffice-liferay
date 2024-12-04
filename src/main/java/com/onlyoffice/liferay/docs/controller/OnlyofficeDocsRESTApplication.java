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

import com.onlyoffice.liferay.docs.controller.reader.CallbackMessageReader;
import com.onlyoffice.liferay.docs.controller.reader.SaveAsRequestMessageReader;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.Application;

@Component(
        property = {
                JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE + "=/onlyoffice-docs",
                JaxrsWhiteboardConstants.JAX_RS_NAME + "=OnlyofficeDocsREST.Application",
                "liferay.access.control.disable=true",
                "auth.verifier.guest.allowed=true",
                "liferay.auth.verifier=false",
                "liferay.oauth2=false"
        },
        service = Application.class
)
public class OnlyofficeDocsRESTApplication extends Application {
        @Reference
        private CallbackController callbackController;
        @Reference
        private DownloadController downloadController;
        @Reference
        private FeatureController featureController;

        public Set<Object> getSingletons() {
                Set<Object> singletons = new HashSet<>();

                singletons.add(new CallbackMessageReader());
                singletons.add(new SaveAsRequestMessageReader());
                singletons.add(callbackController);
                singletons.add(downloadController);
                singletons.add(featureController);

                return singletons;
        }
}
