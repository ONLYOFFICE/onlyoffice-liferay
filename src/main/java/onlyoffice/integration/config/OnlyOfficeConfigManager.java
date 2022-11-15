/**
 *
 * (c) Copyright Ascensio System SIA 2022
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

package onlyoffice.integration.config;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = OnlyOfficeConfiguration.class)
@Component(
    configurationPid = {
        "onlyoffice.integration.config.OnlyOfficeConfiguration"
    },
    immediate = true,
    service = OnlyOfficeConfigManager.class
)
public class OnlyOfficeConfigManager {

    public String getDocUrl() {
        return configuration.docServUrl();
    }

    public String getDocInnerUrl() {
        String url = configuration.docServInnerUrl();
        return url == null || url.isEmpty() ? getDocUrl() : url;
    }

    public String getLiferayUrlOrDefault(String def) {
        String url = configuration.liferayUrl();
        return url == null || url.isEmpty() ? def : url;
    }

    public String getSecret() {
        return configuration.secret();
    }

    public String getJwtHeader() {
        String jwtHeader = configuration.jwtHeader();
        return jwtHeader == null || jwtHeader.isEmpty() ? "Authorization" : jwtHeader;
    }

    public boolean forceSaveEnabled() {
        return configuration.forceSave();
    }

    public boolean webPreview() {
        return configuration.webPreview();
    }

    @Activate
    @Modified
    protected void readConfig(OnlyOfficeConfiguration config) {
        this.configuration = config;
    }

    private OnlyOfficeConfiguration configuration;
}
