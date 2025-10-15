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

package com.onlyoffice.liferay.docs.sdk.client;

import com.onlyoffice.client.ApacheHttpclientDocumentServerClient;
import com.onlyoffice.client.DocumentServerClient;
import com.onlyoffice.client.DocumentServerClientSettings;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.model.settings.security.Security;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        service = DocumentServerClient.class
)
public class DocumentServerClientImpl extends ApacheHttpclientDocumentServerClient {

    public DocumentServerClientImpl() {
        super(DocumentServerClientSettings.builder()
                .baseUrl("base")
                .security(Security.builder()
                        .build())
                .ignoreSSLCertificate(false)
                .build());
    }

    @Reference(service = SettingsManager.class, unbind = "-")
    public void setSettingsManager(final SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    @Reference(service = UrlManager.class, unbind = "-")
    public void setUrlManager(final UrlManager urlManager) {
        this.urlManager = urlManager;
    }

    @Activate
    protected void activate() {
        this.applySettings(DocumentServerClientSettings.builder().build());

        init();
    }
}
