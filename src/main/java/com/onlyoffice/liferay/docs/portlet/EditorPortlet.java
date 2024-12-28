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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.document.library.kernel.exception.FileExtensionException;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.service.UserService;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.onlyoffice.liferay.docs.constants.PortletKeys;
import com.onlyoffice.liferay.docs.utils.FileEntryUtils;
import com.onlyoffice.liferay.docs.utils.PermissionUtils;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.model.documenteditor.Config;
import com.onlyoffice.model.documenteditor.config.document.Type;
import com.onlyoffice.model.documenteditor.config.editorconfig.Mode;
import com.onlyoffice.service.documenteditor.config.ConfigService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import static com.onlyoffice.liferay.docs.utils.FileEntryUtils.LOCKING_TIME;

@Component(
        immediate = true,
        property = {
                "com.liferay.portlet.add-default-resource=true",
                "com.liferay.portlet.display-category=category.hidden",
                "com.liferay.portlet.header-portlet-css=/css/main.css",
                "com.liferay.portlet.instanceable=true",
                "javax.portlet.display-name=OnlyOffice Edit",
                "javax.portlet.init-param.template-path=/",
                "javax.portlet.init-param.view-template=/edit.jsp",
                "javax.portlet.name=" + PortletKeys.EDITOR,
                "javax.portlet.security-role-ref=power-user,user",
                "javax.portlet.resource-bundle=content.Language"
        },
        service = Portlet.class
)
public class EditorPortlet extends AbstractDefaultPortlet {
    private static final Log log = LogFactoryUtil.getLog(EditorPortlet.class);

    @Reference
    private DLAppService dlAppService;
    @Reference
    private UserService userService;
    @Reference
    private PermissionCheckerFactory permissionCheckerFactory;
    @Reference
    private ConfigService configService;
    @Reference
    private DocumentManager documentManager;
    @Reference
    private UrlManager urlManager;
    @Reference
    private FileEntryUtils fileEntryUtils;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doView(final RenderRequest renderRequest, final RenderResponse renderResponse)
            throws IOException, PortletException {
        long fileEntryId = ParamUtil.getLong(renderRequest, "fileEntryId");
        String languageId = LanguageUtil.getLanguageId(renderRequest);
        String lang = LocaleUtil.fromLanguageId(languageId).toLanguageTag();

        try {
            FileEntry fileEntry = dlAppService.getFileEntry(fileEntryId);
            FileVersion fileVersion = fileEntry.getLatestFileVersion();
            String fileName = fileVersion.getFileName();
            Folder folder = fileEntry.getFolder();

            if (documentManager.getDocumentType(fileName) == null) {
                throw new FileExtensionException(fileName);
            }

            User user = userService.getCurrentUser();
            PermissionChecker checker = permissionCheckerFactory.create(user);

            if (documentManager.isEditable(fileName)
                    && fileEntry.containsPermission(checker, ActionKeys.UPDATE)
                    && !fileEntry.hasLock()
            ) {
                ServiceContext serviceContext = ServiceContextThreadLocal.getServiceContext();

                String editingKey = fileEntryUtils.generateEditingKey(fileEntry);

                dlAppService.checkOutFileEntry(
                        fileEntry.getFileEntryId(),
                        fileEntryUtils.createEditorLockOwner(editingKey),
                        LOCKING_TIME,
                        serviceContext
                );
            }

            Config config = configService.createConfig(
                    String.valueOf(fileVersion.getFileVersionId()),
                    Mode.EDIT,
                    Type.DESKTOP
            );

            config.getEditorConfig().setLang(lang);

            String shardkey = config.getDocument().getKey();
            String title = config.getDocument().getTitle();
            boolean canCreateDocument = PermissionUtils.checkFolderPermission(
                    fileEntry.getGroupId(),
                    folder.getFolderId(),
                    ActionKeys.ADD_DOCUMENT
            );

            renderRequest.setAttribute("config", objectMapper.writeValueAsString(config));
            renderRequest.setAttribute("title", title);
            renderRequest.setAttribute("canCreateDocument", canCreateDocument);
            renderRequest.setAttribute("documentServerApiUrl", urlManager.getDocumentServerApiUrl(shardkey));

            super.doView(renderRequest, renderResponse);
        } catch (PortalException e) {
            log.error(e, e);

            SessionErrors.add(renderRequest, e.getClass());
            include("/error.jsp", renderRequest, renderResponse);
        }
    }
}
