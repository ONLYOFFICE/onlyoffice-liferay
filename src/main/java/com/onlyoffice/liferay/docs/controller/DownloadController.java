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

import com.liferay.document.library.kernel.exception.NoSuchFileVersionException;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.petra.io.StreamUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.onlyoffice.liferay.docs.utils.SecurityUtils;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

@Component(service = DownloadController.class)
@Path("/download")
public class DownloadController {
    private static final Log log = LogFactoryUtil.getLog(DownloadController.class);

    @Reference
    private DLAppService dlAppService;
    @Reference
    private SettingsManager settingsManager;
    @Reference
    private JwtManager jwtManager;

    @GET
    @Path("/{groupId}/{uuid}")
    public Response download(final @Context HttpHeaders headers,
                             final @PathParam("groupId") long groupId,
                             final @PathParam("uuid") String uuid,
                             final @QueryParam("version") String version,
                             final @QueryParam("userId") long userId
    ) {
        if (settingsManager.isSecurityEnabled() && !verifyJwtAuthorization(headers)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        try {
            return SecurityUtils.runAs(new SecurityUtils.RunAsWork<Response>() {
                public Response doWork() throws Exception {

                    FileEntry fileEntry = dlAppService.getFileEntryByUuidAndGroupId(uuid, groupId);

                    FileVersion fileVersion;
                    if (version.equals("PWC")) {
                        fileVersion = fileEntry.getLatestFileVersion();
                    } else {
                        List<FileVersion> fileVersions = fileEntry.getFileVersions(WorkflowConstants.STATUS_ANY);
                        fileVersion = fileVersions.stream()
                                .filter(value -> {
                                    return value.getVersion().equals(version);
                                })
                                .findFirst()
                                .orElse(null);
                    }

                    if (fileVersion == null) {
                        throw new NoSuchFileVersionException(
                                MessageFormat.format(
                                        "No FileVersion exists with the key (uuid={0}, groupId={1}, version={2})",
                                        uuid,
                                        groupId,
                                        version
                                )
                        );
                    }

                    InputStream inputStream = fileVersion.getContentStream(false);

                    return Response.ok(
                            (StreamingOutput) outputStream -> {
                                StreamUtil.transfer(inputStream, outputStream);
                            })
                            .header("Content-Disposition", "attachment; filename=\"" + fileVersion.getFileName() + "\"")
                            .header("Content-Length", fileVersion.getSize())
                            .header("Content-Type", fileVersion.getMimeType())
                            .build();
                }
            }, userId);
        } catch (Exception e) {
            log.error(e, e);

            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    private boolean verifyJwtAuthorization(final HttpHeaders headers) {
        String securityHeader = settingsManager.getSecurityHeader();
        String header = headers.getHeaderString(securityHeader);
        String authorizationPrefix = settingsManager.getSecurityPrefix();
        String token = (header != null && header.startsWith(authorizationPrefix))
                ? header.substring(authorizationPrefix.length()) : header;

        if (token == null || token.isEmpty()) {
            return false;
        }

        try {
            jwtManager.verify(token);
        } catch (Exception e) {
            return false;
        }

        return true;
    }
}
