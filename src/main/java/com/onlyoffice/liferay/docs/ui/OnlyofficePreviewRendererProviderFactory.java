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

package com.onlyoffice.liferay.docs.ui;

import com.liferay.document.library.preview.DLPreviewRendererProvider;
import com.liferay.portal.kernel.util.HashMapDictionary;
import com.onlyoffice.manager.settings.SettingsManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletContext;

@Component(
        immediate = true, service = OnlyofficePreviewRendererProviderFactory.class
)
public class OnlyofficePreviewRendererProviderFactory {

    public Set<String> getMimeTypes() {
        return new HashSet<>(Arrays.asList(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.oasis.opendocument.text",
            "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.oasis.opendocument.presentation",
            "application/msword",
            "application/vnd.ms-excel",
            "application/vnd.ms-powerpoint",
            "text/csv",
            "text/rtf",
            "application/rtf",
            "text/plain",
            "application/pdf"
        ));
    }

    @Activate
    protected void activate(BundleContext bundleContext) {
        Dictionary<String, Object> properties = new HashMapDictionary<>();

        properties.put("service.ranking", 100);
        properties.put("content.type", getMimeTypes().toArray());

        _dlPreviewRendererProviderServiceRegistration =
            bundleContext.registerService(
                DLPreviewRendererProvider.class,
                new OnlyofficePreviewRendererProvider(_servletContext, settingsManager),
                properties
            );
    }

    @Deactivate
    protected void deactivate() {
        _dlPreviewRendererProviderServiceRegistration.unregister();
    }

    private ServiceRegistration<DLPreviewRendererProvider>
    _dlPreviewRendererProviderServiceRegistration;

    @Reference
    private SettingsManager settingsManager;

    @Reference(
        target = "(osgi.web.symbolicname=com.onlyoffice.liferay-docs)"
    )
    private ServletContext _servletContext;

}
