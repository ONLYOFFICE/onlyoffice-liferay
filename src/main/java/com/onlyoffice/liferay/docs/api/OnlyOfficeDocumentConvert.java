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

package com.onlyoffice.liferay.docs.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.document.library.kernel.service.DLAppLocalServiceUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.request.RequestManager;
import com.onlyoffice.model.convertservice.ConvertRequest;
import com.onlyoffice.model.convertservice.ConvertResponse;
import com.onlyoffice.service.convert.ConvertService;
import org.apache.hc.core5.http.HttpEntity;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Locale;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component(
    immediate = true,
    property = {
        "osgi.http.whiteboard.context.path=/",
        "osgi.http.whiteboard.servlet.pattern=/onlyoffice/convert/*"
    },
    service = Servlet.class
)
public class OnlyOfficeDocumentConvert extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Log _log = LogFactoryUtil.getLog(OnlyOfficeDocumentConvert.class);

    @Reference
    private DLAppLocalService _dlApp;
    @Reference
    private ConvertService convertService;
    @Reference
    private RequestManager requestManager;
    @Reference
    private DocumentManager documentManger;
    @Reference
    private PermissionCheckerFactory _permissionFactory;

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {
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

        Long fileEntryId = ParamUtil.getLong(request , "fileId");
        String key = ParamUtil.getString(request , "key");
        String fn = ParamUtil.getString(request , "fileName");
        Locale locale = user.getLocale();

        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();

        try {
            FileEntry fileEntry = DLAppLocalServiceUtil.getFileEntry(fileEntryId);

            PermissionChecker checker = _permissionFactory.create(user);
            if (!fileEntry.containsPermission(checker, ActionKeys.VIEW)
                    || !fileEntry.getFolder().containsPermission(checker, ActionKeys.ADD_DOCUMENT)) {
                throw new Exception("User don't have rights");
            }

            ConvertRequest convertRequest = ConvertRequest.builder()
                    .async(true)
                    .key(Long.toString(fileEntry.getFileEntryId()) + key)
                    .region(locale.toLanguageTag())
                    .build();

            Long fileVersionId = fileEntry.getFileVersion().getFileVersionId();

            ConvertResponse convertResponse = convertService.processConvert(
                    convertRequest,
                    String.valueOf(fileVersionId)
            );

            if (convertResponse.getEndConvert() != null && convertResponse.getEndConvert()) {
                savefile(request, fileEntry, convertResponse.getFileUrl(), fn);
            } else if (convertResponse.getError() != null) {
                writer.write("{\"error\":\"Unknown conversion error\"}");
                return;
            }

            ObjectMapper objectMapper = new ObjectMapper();

            writer.write(objectMapper.writeValueAsString(convertResponse));
        } catch (Exception ex) {
            _log.error(ex.getMessage(), ex);
            writer.write("{\"error\":\"" + ex.getMessage() + "\"}");
        }
    }


    private void savefile(final HttpServletRequest request, final FileEntry fileEntry, final String url,
                          final String filename) throws Exception {
        User user = PortalUtil.getUser(request);

        _log.info("Trying to download file from URL: " + url);

        requestManager.executeGetRequest(url, new RequestManager.Callback<Void>() {
            @Override
            public Void doWork(final Object response) throws Exception {
                byte[] bytes = FileUtil.getBytes(((HttpEntity) response).getContent());
                InputStream inputStream = new ByteArrayInputStream(bytes);

                ServiceContext serviceContext = ServiceContextFactory.getInstance(
                        OnlyOfficeDocumentConvert.class.getName(),
                        request
                );

                String defaultConvertExtension = documentManger.getDefaultConvertExtension(fileEntry.getFileName());
                String mimeType = MimeTypesUtil.getContentType(filename + "." + defaultConvertExtension);

                if (defaultConvertExtension != null
                        && (defaultConvertExtension.equals("docxf") || defaultConvertExtension.equals("oform"))) {
                    mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                }

                _dlApp.addFileEntry(user.getUserId(), fileEntry.getRepositoryId(), fileEntry.getFolderId(), filename,
                        mimeType, filename, fileEntry.getDescription(), "ONLYOFFICE Convert",
                        inputStream, bytes.length, serviceContext);

                return null;
            }
        });

        _log.info("Document saved.");
    }
}
