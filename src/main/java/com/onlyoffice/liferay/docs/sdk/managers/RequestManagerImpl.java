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

package com.onlyoffice.liferay.docs.sdk.managers;

import com.onlyoffice.manager.request.DefaultRequestManager;
import com.onlyoffice.manager.request.RequestManager;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
    service = RequestManager.class
)
public class RequestManagerImpl extends DefaultRequestManager {

    public RequestManagerImpl() {
        super(null, null, null);
    }

    @Reference(service = SettingsManager.class, unbind = "-")
    public void setSettingsManager(final SettingsManager settingsManager) {
        super.setSettingsManager(settingsManager);
    }

    @Reference(service = JwtManager.class, unbind = "-")
    public void setJwtManager(final JwtManager jwtManager) {
        super.setJwtManager(jwtManager);
    }

    @Reference(service = UrlManager.class, unbind = "-")
    public void setUrlManager(final UrlManager urlManager) {
        super.setUrlManager(urlManager);
    }
}
