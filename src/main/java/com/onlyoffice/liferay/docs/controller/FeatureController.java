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
import com.liferay.document.library.kernel.util.DLUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.onlyoffice.liferay.docs.controller.dto.SaveAsRequest;
import com.onlyoffice.liferay.docs.utils.SecurityUtils;
import com.onlyoffice.manager.request.RequestManager;
import com.onlyoffice.manager.url.UrlManager;
import org.apache.hc.core5.http.HttpEntity;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.File;
import java.io.InputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Component(service = FeatureController.class)
@Produces("application/json")
@Path("/feature")
public class FeatureController {
    private static final Log log = LogFactoryUtil.getLog(FeatureController.class);

    @Context
    private HttpServletRequest httpServletRequest;
    @Reference
    private DLAppService dlAppService;
    @Reference
    private UrlManager urlManager;
    @Reference
    private RequestManager requestManager;

    @POST
    @Path("/save-as")
    public Response saveAs(final SaveAsRequest request) {
        try {
            SecurityUtils.setUserAuthentication(httpServletRequest);
        } catch (PortalException e) {
            log.error(e, e);

            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        FileEntry fileEntry = null;
        try {
            fileEntry = dlAppService.getFileEntry(request.getFileEntryId());
        } catch (PortalException e) {
            log.error(e, e);

            return Response.status(Response.Status.NOT_FOUND).build();
        }

        String fileName = fileEntry.getFileName();
        String fileUrl = urlManager.replaceToInnerDocumentServerUrl(request.getFileUrl());

        String uniqueFileName = DLUtil.getUniqueFileName(
                fileEntry.getRepositoryId(),
                fileEntry.getFolderId(),
                fileName.substring(0, fileName.lastIndexOf(".") + 1) + request.getFileType()
        );

        File sourceFile = null;
        try {
            sourceFile = requestManager.executeGetRequest(fileUrl, new RequestManager.Callback<File>() {
                @Override
                public File doWork(final Object response) throws Exception {
                    InputStream inputStream = ((HttpEntity) response).getContent();

                    return FileUtil.createTempFile(inputStream);
                }
            });

            dlAppService.addFileEntry(
                    fileEntry.getRepositoryId(),
                    fileEntry.getFolderId(),
                    uniqueFileName,
                    MimeTypesUtil.getContentType(sourceFile),
                    uniqueFileName,
                    null,
                    null,
                    sourceFile,
                    ServiceContextThreadLocal.getServiceContext()
            );
        } catch (PortalException e) {
            log.error(e, e);

            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (Exception e) {
            log.error(e, e);

            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        return Response.ok("{\"fileName\":\"" + uniqueFileName + "\"}").build();
    }
}
