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
import com.liferay.document.library.kernel.service.DLAppLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StreamUtil;
import com.onlyoffice.liferay.docs.OnlyOfficeHasher;
import com.onlyoffice.liferay.docs.OnlyOfficeParsingUtils;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.service.documenteditor.callback.CallbackService;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component(
        immediate = true,
        property = {
                "osgi.http.whiteboard.context.path=/",
                "osgi.http.whiteboard.servlet.pattern=/onlyoffice/doc/*"
        },
        service = Servlet.class
)
public class OnlyOfficeDocumentApi extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactoryUtil.getLog(OnlyOfficeDocumentApi.class);

    @Reference
    private OnlyOfficeHasher hasher;
    @Reference
    private OnlyOfficeParsingUtils parsingUtils;
    @Reference
    private SettingsManager settingsManager;
    @Reference
    private CallbackService callbackService;


    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {

        String key = ParamUtil.getString(request, "key");
        Long fileVersionId = hasher.validate(key);

        try {
            FileVersion fileVersion = DLAppLocalServiceUtil.getFileVersion(fileVersionId);

            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileVersion.getFileName() + "\"");
            response.setHeader("Content-Length", Long.toString(fileVersion.getSize()));
            response.setContentType(fileVersion.getMimeType());

            StreamUtil.transfer(fileVersion.getContentStream(false), response.getOutputStream());
        } catch (PortalException e) {
            log.error(e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {

        String error = null;

        String key = ParamUtil.getString(request, "key");
        Long fileEntryId = hasher.validate(key);

        try {
            String body = parsingUtils.getBody(request.getInputStream());

            if (body.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                error = "Empty body";
            } else {
                ObjectMapper mapper = new ObjectMapper();

                Callback callback = mapper.readValue(body, Callback.class);

                String authorizationHeader = request.getHeader(settingsManager.getSecurityHeader());

                callback = callbackService.verifyCallback(callback, authorizationHeader);

                callbackService.processCallback(callback, String.valueOf(fileEntryId));
            }
        } catch (Exception ex) {
            error = "Unable to process ONLYOFFICE response: " + ex.getMessage();
            log.error(error, ex);
        }

        try {
            JSONObject respBody = new JSONObject();
            if (error != null) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
}
