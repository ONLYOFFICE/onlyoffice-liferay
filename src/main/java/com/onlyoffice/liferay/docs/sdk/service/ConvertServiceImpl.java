/**
 *
 * (c) Copyright Ascensio System SIA 2025
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

package com.onlyoffice.liferay.docs.sdk.service;

import com.onlyoffice.client.DocumentServerClient;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.service.convert.ConvertService;
import com.onlyoffice.service.convert.DefaultConvertServiceV2;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;


@Component(
        service = ConvertService.class
)
public class ConvertServiceImpl extends DefaultConvertServiceV2 implements ConvertService {
    public ConvertServiceImpl() {
        super(null, null, null);
    }

    @Reference(service = DocumentManager.class, unbind = "-")
    public void setDocumentManager(final DocumentManager documentManager) {
        super.setDocumentManager(documentManager);
    }

    @Reference(service = UrlManager.class, unbind = "-")
    public void setUrlManager(final UrlManager urlManager) {
        super.setUrlManager(urlManager);
    }

    @Reference(service = DocumentServerClient.class, unbind = "-")
    public void setDocumentServerClient(final DocumentServerClient documentServerClient) {
        super.setDocumentServerClient(documentServerClient);
    }
}
