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

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.onlyoffice.liferay.docs.utils.FileEntryUtils;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.service.documenteditor.callback.CallbackService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

@Component(service = CallbackController.class)
@Consumes("application/json")
@Produces("application/json")
@Path("/callback")
public class CallbackController {
    private static final Log log = LogFactoryUtil.getLog(CallbackController.class);

    @Reference
    private CallbackService callbackService;
    @Reference
    private SettingsManager settingsManager;
    @Reference
    private FileEntryUtils fileEntryUtils;

    @POST
    @Path("/{groupId}/{uuid}")
    public Response callback(
            final @Context HttpHeaders headers,
            final @PathParam("groupId") long groupId,
            final @PathParam("uuid") String uuid,
            final @QueryParam("userId") String userId,
            final Callback callback) {
        String authorizationHeader = headers.getHeaderString(settingsManager.getSecurityHeader());

        Callback verifiedCallback;
        try {
            verifiedCallback = callbackService.verifyCallback(callback, authorizationHeader);
        } catch (Exception e) {
            log.error(e, e);

            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        try {
            callbackService.processCallback(
                    verifiedCallback,
                    fileEntryUtils.formatFileId(groupId, uuid)
            );
        } catch (Exception e) {
            log.error(e, e);

            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        return Response.ok("{\"error\": 0}").build();
    }
}
