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

package com.onlyoffice.liferay.docs.sdk.service;

import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.model.common.User;
import com.onlyoffice.model.documenteditor.config.document.Permissions;
import com.onlyoffice.service.documenteditor.config.ConfigService;
import com.onlyoffice.service.documenteditor.config.DefaultConfigService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        service = ConfigService.class
)
public class ConfigServiceImpl extends DefaultConfigService {
    @Reference
    private PermissionCheckerFactory permissionCheckerFactory;
    @Reference
    private DLAppService dlAppService;

    public ConfigServiceImpl() {
        super(null, null, null, null);
    }

    @Reference(service = SettingsManager.class, unbind = "-")
    public void setSettingsManager(final SettingsManager settingsManager) {
        super.setSettingsManager(settingsManager);
    }

    @Reference(service = DocumentManager.class, unbind = "-")
    public void setDocumentManager(final DocumentManager documentManager) {
        super.setDocumentManager(documentManager);
    }

    @Reference(service = JwtManager.class, unbind = "-")
    public void setJwtManager(final JwtManager jwtManager) {
        super.setJwtManager(jwtManager);
    }

    @Reference(service = UrlManager.class, unbind = "-")
    public void setUrlManager(final UrlManager urlManager) {
        super.setUrlManager(urlManager);
    }

    @Override
    public Permissions getPermissions(final String fileId) {
        ServiceContext serviceContext = ServiceContextThreadLocal.getServiceContext();
        Long userId = serviceContext.getUserId();

        try {
            com.liferay.portal.kernel.model.User user = UserLocalServiceUtil.getUserById(userId);
            FileVersion fileVersion = dlAppService.getFileVersion(Long.parseLong(fileId));
            FileEntry fileEntry = fileVersion.getFileEntry();
            String fileName = getDocumentManager().getDocumentName(fileId);

            PermissionChecker checker = permissionCheckerFactory.create(user);

            boolean editPermission = fileEntry.containsPermission(checker, ActionKeys.UPDATE);
            Boolean isEditable = super.getDocumentManager().isEditable(fileName)
                    || super.getDocumentManager().isFillable(fileName);

            return Permissions.builder()
                    .edit(editPermission && isEditable)
                    .build();
        } catch (PortalException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public User getUser() {
        ServiceContext serviceContext = ServiceContextThreadLocal.getServiceContext();
        Long userId = serviceContext.getUserId();

        com.liferay.portal.kernel.model.User user;

        try {
            user = UserLocalServiceUtil.getUserById(userId);
        } catch (PortalException e) {
            throw new RuntimeException(e);
        }

        return User.builder()
                .id(String.valueOf(user.getUserId()))
                .name(user.getFullName())
                .build();
    }
}
