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

import com.onlyoffice.liferay.docs.config.OnlyOfficeConfiguration;
import com.onlyoffice.manager.settings.DefaultSettingsManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.settings.SettingsConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

@Component(
    configurationPid = {
            "com.onlyoffice.liferay.docs.config.OnlyOfficeConfiguration"
    },
    immediate = true,
    service = SettingsManager.class
)
public class SettingsManagerImpl extends DefaultSettingsManager {

    @Activate
    @Modified
    protected void readConfig(final OnlyOfficeConfiguration config) {
        this.configuration = config;
    }

    @Override
    public String getSetting(final String name) {
        switch(name) {
            case SettingsConstants.URL:
                return configuration.docServUrl();
            case SettingsConstants.INNER_URL:
                return configuration.docServInnerUrl();
            case SettingsConstants.SECURITY_KEY:
                return configuration.secret();
            case SettingsConstants.SECURITY_HEADER:
                return configuration.jwtHeader();
            case SettingsConstants.PRODUCT_INNER_URL:
                return configuration.liferayUrl();
            case "customization.forcesave":
                return String.valueOf(configuration.forceSave());
            case "preview":
                return String.valueOf(configuration.webPreview());
        }

        return null;
    }

    @Override
    public void setSetting(final String name, final String value) {
    }

    private OnlyOfficeConfiguration configuration;
}
