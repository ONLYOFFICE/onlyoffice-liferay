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

package com.onlyoffice.liferay.docs.portlet;

import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.service.UserService;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.util.ParamUtil;
import com.onlyoffice.liferay.docs.constants.PortletKeys;
import com.onlyoffice.manager.document.DocumentManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

@Component(
        immediate = true,
        property = {
                "com.liferay.portlet.add-default-resource=true",
                "com.liferay.portlet.display-category=category.hidden",
                "com.liferay.portlet.header-portlet-css=/css/main.css",
                "com.liferay.portlet.instanceable=true",
                "javax.portlet.init-param.template-path=/",
                "javax.portlet.init-param.view-template=/convert.jsp",
                "javax.portlet.name=" + PortletKeys.CONVERT,
                "javax.portlet.security-role-ref=power-user,user",
                "javax.portlet.resource-bundle=content.Language"
        },
        service = Portlet.class
)
public class ConvertPortlet extends MVCPortlet {
    private static final Log log = LogFactoryUtil.getLog(EditorPortlet.class);

    @Reference
    private UserService userService;
    @Reference
    private PermissionCheckerFactory permissionCheckerFactory;
    @Reference
    private DLAppService dlAppService;
    @Reference
    private DocumentManager documentManager;

    @Override
    public void doView(final RenderRequest renderRequest, final RenderResponse renderResponse)
            throws IOException, PortletException {
        long fileEntryId = ParamUtil.getLong(renderRequest, "fileEntryId");

        try {
            FileEntry fileEntry = dlAppService.getFileEntry(fileEntryId);
            FileVersion fileVersion = fileEntry.getFileVersion();
            String fileName = fileVersion.getFileName();
            String baseFileName = documentManager.getBaseName(fileName);
            String defaultConvertExtension = documentManager.getDefaultConvertExtension(fileName);
            Folder folder = fileEntry.getFolder();

            User user = userService.getCurrentUser();
            PermissionChecker permissionChecker = permissionCheckerFactory.create(user);

            if (!folder.containsPermission(permissionChecker, ActionKeys.ADD_DOCUMENT)) {
                throw new PrincipalException.MustHavePermission(
                        user.getUserId(),
                        folder.getClass().getName(),
                        folder.getFolderId(),
                        ActionKeys.ADD_DOCUMENT
                );
            }

            String newFileName = baseFileName + "." + defaultConvertExtension;

            renderRequest.setAttribute("fileEntryId", fileEntry.getFileEntryId());
            renderRequest.setAttribute("version", fileVersion.getVersion());
            renderRequest.setAttribute("fileName", fileName);
            renderRequest.setAttribute("newFileName", newFileName);

            super.doView(renderRequest, renderResponse);
        } catch (PortalException e) {
            log.error(e, e);

            SessionErrors.add(renderRequest, e.getClass());
            include("/error.jsp", renderRequest, renderResponse);
        }
    }
}
