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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.document.library.preview.DLPreviewRenderer;
import com.liferay.document.library.preview.DLPreviewRendererProvider;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.model.documenteditor.Config;
import com.onlyoffice.model.documenteditor.config.document.Type;
import com.onlyoffice.model.documenteditor.config.editorconfig.Mode;
import com.onlyoffice.service.documenteditor.config.ConfigService;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Optional;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;

public class OnlyofficePreviewRendererProvider implements DLPreviewRendererProvider {
    private final ServletContext servletContext;
    private final ConfigService configService;
    private final SettingsManager settingsManager;
    private final UrlManager urlManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OnlyofficePreviewRendererProvider(final ServletContext servletContext, final ConfigService configService,
                                             final SettingsManager settingsManager, final UrlManager urlManager) {
        this.servletContext = servletContext;
        this.configService = configService;
        this.settingsManager = settingsManager;
        this.urlManager = urlManager;
    }

    @Override
    public Optional<DLPreviewRenderer> getPreviewDLPreviewRendererOptional(final FileVersion fileVersion) {
        if (!settingsManager.getSettingBoolean("preview", false)) {
            return Optional.empty();
        }

        return Optional.of((request, response) -> {
            String languageId = LanguageUtil.getLanguageId(request);
            Locale locale = LocaleUtil.fromLanguageId(languageId);
            boolean version = request.getAttribute(WebKeys.DOCUMENT_LIBRARY_FILE_VERSION) != null;

            Config config = getPreviewConfig(fileVersion, locale, version);
            String shardkey = config.getDocument().getKey();

            request.setAttribute("config", objectMapper.writeValueAsString(config));
            request.setAttribute("documentServerApiUrl", urlManager.getDocumentServerApiUrl(shardkey));

            RequestDispatcher requestDispatcher = servletContext.getRequestDispatcher("/preview.jsp");

            requestDispatcher.include(request, response);
        });
    }

    @Override
    public Optional<DLPreviewRenderer> getThumbnailDLPreviewRendererOptional(final FileVersion fileVersion) {
        // TODO Auto-generated method stub
        return Optional.empty();
    }

    private Config getPreviewConfig(final FileVersion fileVersion, final Locale locale, final boolean version) {
        String title = fileVersion.getFileName();
        if (version) {
            title = MessageFormat.format(
                    "{0} ({1} {2})",
                    fileVersion.getFileName(),
                    LanguageUtil.get(locale, "version"),
                    fileVersion.getVersion()
            );
        }

        Config config = configService.createConfig(
                String.valueOf(fileVersion.getFileVersionId()),
                Mode.VIEW,
                Type.EMBEDDED
        );

        config.getEditorConfig().setLang(locale.toLanguageTag());
        config.getDocument().setTitle(title);

        return config;
    }
}
