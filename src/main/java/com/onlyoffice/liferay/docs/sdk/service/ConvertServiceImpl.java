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

import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.request.RequestManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.service.convert.ConvertService;
import com.onlyoffice.service.convert.DefaultConvertService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;


@Component(
        service = ConvertService.class
)
public class ConvertServiceImpl extends DefaultConvertService implements ConvertService {
    public ConvertServiceImpl() {
        super(null, null, null, null);
    }

    @Reference(service = SettingsManager.class, unbind = "-")
    public void setSettingsManager(final SettingsManager settingsManager) {
        super.setSettingsManager(settingsManager);
    }

    @Reference(service = DocumentManager.class, unbind = "-")
    public void setDocumentManager(final DocumentManager documentManager) {
        super.setDocumentManager(documentManager);
    }

    @Reference(service = RequestManager.class, unbind = "-")
    public void setRequestManager(final RequestManager requestManager) {
        super.setRequestManager(requestManager);
    }

    @Reference(service = UrlManager.class, unbind = "-")
    public void setUrlManager(final UrlManager urlManager) {
        super.setUrlManager(urlManager);
    }
}
