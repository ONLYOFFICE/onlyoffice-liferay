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

import com.liferay.document.library.preview.DLPreviewRenderer;
import com.liferay.document.library.preview.DLPreviewRendererProvider;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.util.WebKeys;
import com.onlyoffice.manager.settings.SettingsManager;

import java.util.Optional;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;

public class OnlyofficePreviewRendererProvider implements DLPreviewRendererProvider {
    private final SettingsManager settingsManager;
    private final ServletContext servletContext;

    public OnlyofficePreviewRendererProvider(final ServletContext servletContext,
                                             final SettingsManager settingsManager) {
        this.servletContext = servletContext;
        this.settingsManager = settingsManager;
    }

    @Override
    public Optional<DLPreviewRenderer> getPreviewDLPreviewRendererOptional(final FileVersion fileVersion) {
        if (!settingsManager.getSettingBoolean("preview", false)) {
            return Optional.empty();
        }

        return Optional.of((request, response) -> {
            RequestDispatcher requestDispatcher = servletContext.getRequestDispatcher("/preview.jsp");

            request.setAttribute("fileEntryId", fileVersion.getFileEntryId());

            if (request.getAttribute(WebKeys.DOCUMENT_LIBRARY_FILE_VERSION) != null) {
                request.setAttribute("version", fileVersion.getVersion());
            }

            requestDispatcher.include(request, response);
        });
    }

    @Override
    public Optional<DLPreviewRenderer> getThumbnailDLPreviewRendererOptional(final FileVersion fileVersion) {
        // TODO Auto-generated method stub
        return Optional.empty();
    }
}
