/**
 *
 * (c) Copyright Ascensio System SIA 2023
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
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.util.WebKeys;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import onlyoffice.integration.config.OnlyOfficeConfigManager;

@Component(
        property = {
            "service.ranking:Integer=100"
        },
        service = DLPreviewRendererProvider.class
)
public class OnlyofficePreviewRendererProvider implements DLPreviewRendererProvider {

    @Override
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

    @Override
    public DLPreviewRenderer getPreviewDLPreviewRenderer(FileVersion fileVersion) {	
        if (!_config.webPreview()) {
            return null;
        }

        return (request, response) -> {
            RequestDispatcher requestDispatcher = this.servletContext.getRequestDispatcher("/preview.jsp");

            request.setAttribute("fileEntryId", fileVersion.getFileEntryId());

            if (request.getAttribute(WebKeys.DOCUMENT_LIBRARY_FILE_VERSION) != null) {
                request.setAttribute("version", fileVersion.getVersion());
            }

            requestDispatcher.include(request, response);
        };
    }

    @Override
    public DLPreviewRenderer getThumbnailDLPreviewRenderer(FileVersion fileVersion) {
        // TODO Auto-generated method stub
        return null;
    }

    @Reference(target = "(osgi.web.symbolicname=onlyoffice.integration.web)")
    protected ServletContext servletContext;

    @Reference
    private OnlyOfficeConfigManager _config;
}
