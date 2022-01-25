/**
 *
 * (c) Copyright Ascensio System SIA 2021
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

package onlyoffice.integration.api;

import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.document.library.kernel.service.DLAppLocalServiceUtil;
import com.liferay.document.library.kernel.util.DLUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.util.FileUtil;

import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.liferay.portal.kernel.util.PortalUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import onlyoffice.integration.OnlyOfficeParsingUtils;
import onlyoffice.integration.OnlyOfficeUtils;
import onlyoffice.integration.permission.OnlyOfficePermissionUtils;

@Component(
    immediate = true,
    property = {
        "osgi.http.whiteboard.context.path=/",
        "osgi.http.whiteboard.servlet.pattern=/onlyoffice/api/*"
    },
    service = Servlet.class
)
public class OnlyOfficeApi extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        String type = request.getParameter("type");
            if (type != null) {
                switch (type.toLowerCase())
                {
                    case "save-as":
                        saveAs(request, response);
                        break;
                    default:
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                }
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
    }

    private void saveAs (HttpServletRequest request, HttpServletResponse response) throws IOException { 
        User user;

        try {
            user = PortalUtil.getUser(request);
            if (user == null) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String body = _parsingUtils.getBody(request.getInputStream());

        File sourceFile = null;

        try {
            JSONObject bodyJson = new JSONObject(body);

            String url = bodyJson.getString("url");
            String fileType = bodyJson.getString("fileType");
            Long fileEntryId = bodyJson.getLong("fileEntryId");
            
            if (url == null || url.isEmpty() || fileType == null || fileType.isEmpty() || fileEntryId == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            FileEntry file = DLAppLocalServiceUtil.getFileEntry(fileEntryId);

            if (!OnlyOfficePermissionUtils.saveAs(file, user)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            String fileName = file.getFileName();

            String uniqueFileName = DLUtil.getUniqueFileName(
                    file.getRepositoryId(),
                    file.getFolderId(), 
                    fileName.substring(0, fileName.lastIndexOf(".") + 1) + fileType
            );

            ServiceContext serviceContext = ServiceContextFactory.getInstance(OnlyOfficeDocumentConvert.class.getName(), request);

            url = _utils.replaceDocServerURLToInternal(url);

            URLConnection connection = new URL(url).openConnection();
            InputStream inputStream = connection.getInputStream();

            sourceFile = FileUtil.createTempFile(inputStream);
            String mimeType = MimeTypesUtil.getContentType(sourceFile);

            _dlApp.addFileEntry(user.getUserId(), file.getRepositoryId(), file.getFolderId(), uniqueFileName,
                    mimeType, uniqueFileName, null, null, sourceFile, serviceContext);

            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write("{\"fileName\":\""+ uniqueFileName +"\"}");
        } catch (Exception e) {
            throw new IOException(e.getMessage(),e);
        } finally {
            if (sourceFile != null && sourceFile.exists()) {
                sourceFile.delete();
            }
        }
    }

    private static final long serialVersionUID = 1L;

    private static final Log _log = LogFactoryUtil.getLog(OnlyOfficeApi.class);

    @Reference
    private OnlyOfficeUtils _utils;

    @Reference
    private OnlyOfficeParsingUtils _parsingUtils;

    @Reference
    private DLAppLocalService _dlApp;
}