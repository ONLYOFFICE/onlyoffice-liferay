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

package com.onlyoffice.liferay.docs.ui;

import com.liferay.document.library.preview.DLPreviewRendererProvider;
import com.liferay.portal.kernel.util.HashMapDictionary;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.model.common.Format;
import com.onlyoffice.service.documenteditor.config.ConfigService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletContext;

@Component(
        immediate = true,
        service = OnlyofficePreviewRendererProviderFactory.class
)
public class OnlyofficePreviewRendererProviderFactory {
    private static final int ONLYOFFICE_PREVIEW_SERVICE_RANKING = 100;

    private ServiceRegistration<DLPreviewRendererProvider> dlPreviewRendererProviderServiceRegistration;

    @Reference(
            target = "(osgi.web.symbolicname=com.onlyoffice.liferay-docs)"
    )
    private ServletContext servletContext;
    @Reference
    private ConfigService configService;
    @Reference
    private SettingsManager settingsManager;
    @Reference
    private UrlManager urlManager;
    @Reference
    private DocumentManager documentManager;

    public Set<String> getMimeTypes() {
       Set<String> mimeTypes = new HashSet<>();

       for (Format format : documentManager.getFormats()) {
           if (format.getActions().contains("view")) {
               mimeTypes.addAll(format.getMime());
           }
       }

       return mimeTypes;
    }

    @Activate
    protected void activate(final BundleContext bundleContext) {
        Dictionary<String, Object> properties = new HashMapDictionary<>();

        properties.put("service.ranking", ONLYOFFICE_PREVIEW_SERVICE_RANKING);
        properties.put("content.type", getMimeTypes().toArray());

        dlPreviewRendererProviderServiceRegistration =
            bundleContext.registerService(
                DLPreviewRendererProvider.class,
                new OnlyofficePreviewRendererProvider(servletContext, configService, settingsManager, urlManager),
                properties
            );
    }

    @Deactivate
    protected void deactivate() {
        dlPreviewRendererProviderServiceRegistration.unregister();
    }
}
