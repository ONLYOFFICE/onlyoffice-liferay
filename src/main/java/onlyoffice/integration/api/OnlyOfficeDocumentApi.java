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
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.liferay.document.library.kernel.model.DLVersionNumberIncrease;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.document.library.kernel.service.DLAppLocalServiceUtil;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.document.library.kernel.service.DLFileEntryLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.lock.Lock;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StreamUtil;

import onlyoffice.integration.OnlyOfficeHasher;
import onlyoffice.integration.OnlyOfficeJWT;
import onlyoffice.integration.OnlyOfficeUtils;

@Component(
    immediate = true,
    property = {
        "osgi.http.whiteboard.context.path=/",
        "osgi.http.whiteboard.servlet.pattern=/onlyoffice/doc/*"
    },
    service = Servlet.class
)
public class OnlyOfficeDocumentApi extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {

        String key = ParamUtil.getString(request, "key");
        Long fileEntryId = _hasher.validate(key);

        FileEntry fileEntry;
        try {
            fileEntry = DLAppLocalServiceUtil.getFileEntry(fileEntryId);

            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileEntry.getFileName() + "\"");
            response.setHeader("Content-Length", Long.toString(fileEntry.getSize()));
            response.setContentType(fileEntry.getMimeType());

            StreamUtil.transfer(fileEntry.getContentStream(), response.getOutputStream());
        } catch (PortalException e) {
            _log.error(e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {

        String error = null;

        String key = ParamUtil.getString(request, "key");
        Long fileEntryId = _hasher.validate(key);

        try {
            FileEntry fileEntry = _dlApp.getFileEntry(fileEntryId);

            String body = getBody(request.getInputStream());
            if (body.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                error = "Empty body";
            } else {
                JSONObject jsonObj = new JSONObject(body);

                if (_jwt.isEnabled()) {
                    jsonObj = _jwt.validate(jsonObj, request);
                }

                processData(fileEntry, jsonObj, request);
            }
        } catch (Exception ex) {
            _log.error("Unable to process ONLYOFFICE response: " + ex.getMessage(), ex);
            error = ex.getMessage();
        }

        try {
            JSONObject respBody = new JSONObject();
            if (error != null) {
                response.setStatus(500);
                respBody.put("error", 1);
                respBody.put("message", error);
            } else {
                respBody.put("error", 0);
            }

            response.getWriter().write(respBody.toString(2));
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
    
    private String getBody(InputStream is) {
        try {
            Scanner s = new Scanner(is);
            s.useDelimiter("\\A");
            String result = s.hasNext() ? s.next() : "";
            s.close();
            is.close();

            return result;
        } catch (IOException e) {
            _log.error(e.getMessage(), e);
        }
        return "";
    }
    
    private void processData(FileEntry fileEntry, JSONObject body, HttpServletRequest request)
        throws JSONException, PortalException, Exception {
        ServiceContext serviceContext = ServiceContextFactory.getInstance(OnlyOfficeDocumentApi.class.getName(), request);

        Long userId = (long) -1;

        if (body.has("users")) {
            JSONArray users = body.getJSONArray("users");
            userId = users.getLong(0);
        }

        switch(body.getInt("status")) {
            case 0:
                _log.error("ONLYOFFICE has reported that no doc with the specified key can be found");

                if (fileEntry.isCheckedOut()) {
                    setUserThreadLocal(fileEntry.getLock().getUserId());
                    _dlAppService.cancelCheckOut(fileEntry.getFileEntryId());
                }

                break;
            case 1:
                if (!fileEntry.isCheckedOut()) {
                    setUserThreadLocal(userId);
                    _dlAppService.checkOutFileEntry(fileEntry.getFileEntryId(), serviceContext);

                    _log.info("Document opened for editing, locking document");
                } else {
                    _log.info("Document already locked, another user has entered/exited");
                }

                break;
            case 2:
                _log.info("Document updated, changing content");

                String download = _utils.replaceDocServerURLToInternal(body.getString("url"));

                Lock lockFile = fileEntry.getLock();
                if (userId.longValue() != lockFile.getUserId()) {
                    setUserThreadLocal(lockFile.getUserId());
                    _dlAppService.cancelCheckOut(fileEntry.getFileEntryId());

                    setUserThreadLocal(userId);
                    _dlAppService.checkOutFileEntry(fileEntry.getFileEntryId(), serviceContext);
                } else {
                    setUserThreadLocal(userId);
                }

                updateFile(fileEntry, userId, download, serviceContext);

                _log.info("Document saved.");

                break;
            case 3:
                _log.error("ONLYOFFICE has reported that saving the document has failed, unlocking document");
                
                if (fileEntry.isCheckedOut()) {
                    setUserThreadLocal(fileEntry.getLock().getUserId());
                    _dlAppService.cancelCheckOut(fileEntry.getFileEntryId());
                }

                break;
            case 4:
                _log.info("No document updates, unlocking document");

                if (fileEntry.isCheckedOut()) {
                    setUserThreadLocal(fileEntry.getLock().getUserId());
                    _dlAppService.cancelCheckOut(fileEntry.getFileEntryId());
                }

                break;
        }
    }

    private void updateFile(FileEntry fileEntry, Long userId, String url, ServiceContext serviceContext) throws Exception {
        _log.info("Trying to download file from URL: " + url);

        try {
            URLConnection con = new URL(url).openConnection();
            InputStream in = con.getInputStream();

            _dlApp.updateFileEntry(userId, fileEntry.getFileEntryId(), fileEntry.getFileName(), fileEntry.getMimeType(),
                    fileEntry.getTitle(), fileEntry.getDescription(), "ONLYOFFICE Edit",
                    DLVersionNumberIncrease.MINOR, in, con.getContentLength(), serviceContext);

            _dlAppService.checkInFileEntry(fileEntry.getFileEntryId(), DLVersionNumberIncrease.MINOR, "ONLYOFFICE Edit", serviceContext);
        } catch (Exception e) {
            String msg = "Couldn't download or save file: " + e.getMessage();
            _log.error(msg, e);
            throw new Exception(msg);
        }
    }

    private void setUserThreadLocal (Long userId) throws PortalException {
        User user = UserLocalServiceUtil.getUser(userId);

        PermissionChecker permissionChecker;
        permissionChecker = PermissionCheckerFactoryUtil.create(user);

        PrincipalThreadLocal.setName(userId);
        PermissionThreadLocal.setPermissionChecker(permissionChecker);
    }

    private static final long serialVersionUID = 1L;

    private static final Log _log = LogFactoryUtil.getLog(OnlyOfficeDocumentApi.class);

    @Reference
    private OnlyOfficeJWT _jwt;

    @Reference
    private OnlyOfficeHasher _hasher;

    @Reference
    private DLAppLocalService _dlApp;

    @Reference
    private DLFileEntryLocalService _dlFile;

    @Reference
    private DLAppService _dlAppService;

    @Reference
    private OnlyOfficeUtils _utils;
}