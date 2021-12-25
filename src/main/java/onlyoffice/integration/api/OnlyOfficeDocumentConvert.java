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

import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.document.library.kernel.service.DLAppLocalServiceUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;

import onlyoffice.integration.OnlyOfficeConvertUtils;

@Component(
    immediate = true,
    property = {
        "osgi.http.whiteboard.context.path=/",
        "osgi.http.whiteboard.servlet.pattern=/onlyoffice/convert/*"
    },
    service = Servlet.class
)
public class OnlyOfficeDocumentConvert extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        User user;

        try {
            user = PortalUtil.getUser(request);
            if (user == null) {
                throw new Exception("user is null");
            }
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Long versionId = ParamUtil.getLong(request , "fileId");
        String key = ParamUtil.getString(request , "key");
        String fn = ParamUtil.getString(request , "fileName");

        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();

        try {
            FileVersion file = DLAppLocalServiceUtil.getFileVersion(versionId);

            PermissionChecker checker = _permissionFactory.create(user);
            FileEntry fe = file.getFileEntry();
            if (!fe.containsPermission(checker, ActionKeys.VIEW) || !fe.getFolder().containsPermission(checker, ActionKeys.ADD_DOCUMENT)) {
                throw new Exception("User don't have rights");
            }

            JSONObject json = _convert.convert(request, file, key);

            if (json.has("endConvert") && json.getBoolean("endConvert")) {
                savefile(request, file, json.getString("fileUrl"), fn);
            } else if (json.has("error")) {
                writer.write("{\"error\":\"Unknown conversion error\"}");
                return;
            }

            writer.write(json.toString());
        } catch (Exception ex) {
            _log.error(ex.getMessage());
            writer.write("{\"error\":\"" + ex.getMessage() + "\"}");
        }
    }


    private void savefile(HttpServletRequest request, FileVersion file, String url, String filename) throws Exception {
        User user = PortalUtil.getUser(request);

        _log.info("Trying to download file from URL: " + url);

        URLConnection con = new URL(url).openConnection();
        InputStream in = con.getInputStream();
        ServiceContext serviceContext = ServiceContextFactory.getInstance(OnlyOfficeDocumentConvert.class.getName(), request);

        _dlApp.addFileEntry(user.getUserId(), file.getRepositoryId(), file.getFileEntry().getFolderId(), filename,
                _convert.getMimeType(file.getExtension()), filename, file.getDescription(), "ONLYOFFICE Convert",
                in, con.getContentLength(), serviceContext);

        _log.info("Document saved.");
    }

    private static final long serialVersionUID = 1L;

    @Reference
    private DLAppLocalService _dlApp;

    @Reference
    OnlyOfficeConvertUtils _convert;

    @Reference
    private PermissionCheckerFactory _permissionFactory;

    private static final Log _log = LogFactoryUtil.getLog(
            OnlyOfficeDocumentConvert.class);
}