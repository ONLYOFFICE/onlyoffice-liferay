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

package onlyoffice.integration.ui;

import com.liferay.document.library.preview.DLPreviewRenderer;
import com.liferay.document.library.preview.DLPreviewRendererProvider;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.util.WebKeys;

import java.util.Optional;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;

import onlyoffice.integration.config.OnlyOfficeConfigManager;

public class OnlyofficePreviewRendererProvider implements DLPreviewRendererProvider {

    public OnlyofficePreviewRendererProvider(ServletContext servletContext, OnlyOfficeConfigManager onlyOfficeConfigManager) {
        _servletContext = servletContext;
        _config = onlyOfficeConfigManager;
    }

    @Override
    public Optional<DLPreviewRenderer> getPreviewDLPreviewRendererOptional(FileVersion fileVersion) {
        if (!_config.webPreview()) {
            return Optional.empty();
        }

        return Optional.of( (request, response) -> {
            RequestDispatcher requestDispatcher = _servletContext.getRequestDispatcher("/preview.jsp");

            request.setAttribute("fileEntryId", fileVersion.getFileEntryId());

            if (request.getAttribute(WebKeys.DOCUMENT_LIBRARY_FILE_VERSION) != null) {
                request.setAttribute("version", fileVersion.getVersion());
            }

            requestDispatcher.include(request, response);
        });
    }

    @Override
    public Optional<DLPreviewRenderer> getThumbnailDLPreviewRendererOptional(FileVersion fileVersion) {
        // TODO Auto-generated method stub
        return Optional.empty();
    }

    private OnlyOfficeConfigManager _config;
    private final ServletContext _servletContext;

    private static final Log _log = LogFactoryUtil.getLog(OnlyofficePreviewRendererProvider.class);
}
