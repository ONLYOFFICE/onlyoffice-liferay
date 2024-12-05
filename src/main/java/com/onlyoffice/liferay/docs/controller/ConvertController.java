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

package com.onlyoffice.liferay.docs.controller;

import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.onlyoffice.liferay.docs.utils.FileEntryUtils;
import com.onlyoffice.liferay.docs.utils.SecurityUtils;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.model.convertservice.ConvertRequest;
import com.onlyoffice.model.convertservice.ConvertResponse;
import com.onlyoffice.service.convert.ConvertService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Component(service = ConvertController.class)
@Consumes("application/json")
@Produces("application/json")
@Path("/convert")
public class ConvertController {
    private static final Log log = LogFactoryUtil.getLog(ConvertController.class);

    @Context
    private HttpServletRequest httpServletRequest;
    @Reference
    private PermissionCheckerFactory permissionCheckerFactory;
    @Reference
    private DLAppService dlAppService;
    @Reference
    private FileEntryUtils fileEntryUtils;
    @Reference
    private ConvertService convertService;
    @Reference
    private DocumentManager documentManager;

    @POST
    public Response convert(final com.onlyoffice.liferay.docs.controller.dto.ConvertRequest request) {
        User user;
        try {
            user = SecurityUtils.setUserAuthentication(httpServletRequest);
        } catch (PortalException e) {
            log.error(e, e);

            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }

        FileEntry fileEntry;
        FileVersion fileVersion;
        try {
            fileEntry = dlAppService.getFileEntry(request.getFileEntryId());
            fileVersion = fileEntryUtils.getFileVersion(fileEntry, request.getVersion());
        } catch (PortalException e) {
            log.error(e, e);

            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }

        Folder folder = fileEntry.getFolder();
        String fileName = fileVersion.getFileName();
        String baseFileName = documentManager.getBaseName(fileName);
        String defaultConvertExtension = documentManager.getDefaultConvertExtension(fileName);
        String newFileName = baseFileName + "." + defaultConvertExtension;
        Locale locale = user.getLocale();

        PermissionChecker permissionChecker = permissionCheckerFactory.create(user);
        try {
            if (!folder.containsPermission(permissionChecker, ActionKeys.ADD_DOCUMENT)) {
                throw new PrincipalException.MustHavePermission(
                        user.getUserId(),
                        folder.getClass().getName(),
                        folder.getFolderId(),
                        ActionKeys.ADD_DOCUMENT
                );
            }
        } catch (PortalException e) {
            log.error(e, e);

            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }

        ConvertRequest convertRequest = ConvertRequest.builder()
                .async(true)
                .key(fileVersion.getUuid() + "_" + request.getTime())
                .region(locale.toLanguageTag())
                .build();

        ConvertResponse convertResponse = null;
        try {
            convertResponse = convertService.processConvert(
                    convertRequest,
                    String.valueOf(fileVersion.getFileVersionId())
            );
        } catch (Exception e) {
            log.error(e, e);

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }

        if (convertResponse.getError() != null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"" + convertResponse.getError().getDescription() + "\"}")
                    .build();
        }

        if (convertResponse.getEndConvert() != null && convertResponse.getEndConvert()) {
            try {
                fileEntryUtils.createFileEntryFromUrl(
                        newFileName,
                        fileEntry.getRepositoryId(),
                        folder.getFolderId(),
                        convertResponse.getFileUrl()
                );
            } catch (Exception e) {
                log.error(e, e);

                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("{\"error\":\"" + e.getMessage() + "\"}")
                        .build();
            }
        }

        return Response.ok()
                .entity(convertResponse)
                .build();
    }
}
