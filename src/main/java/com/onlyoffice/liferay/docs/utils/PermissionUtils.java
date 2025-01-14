/**
 *
 * (c) Copyright Ascensio System SIA 2025
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

package com.onlyoffice.liferay.docs.utils;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.security.permission.resource.ModelResourcePermission;
import com.liferay.portal.kernel.security.permission.resource.ModelResourcePermissionUtil;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.service.UserServiceUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        immediate = true,
        service = PermissionUtils.class
)
public final class PermissionUtils {
    private static ModelResourcePermission<Folder> folderModelResourcePermission;
    private static PermissionCheckerFactory permissionCheckerFactory;

    @Reference(
            target = "(model.class.name=com.liferay.portal.kernel.repository.model.Folder)",
            unbind = "-"
    )
    public void setFolderModelResourcePermission(final ModelResourcePermission<Folder> modelResourcePermission) {
        folderModelResourcePermission = modelResourcePermission;
    }

    @Reference
    public void setPermissionCheckerFactory(final PermissionCheckerFactory checkerFactory) {
        permissionCheckerFactory = checkerFactory;
    }

    public static boolean checkFolderPermission(final long groupId, final long folderId,
                                                final String actionId) throws PortalException {
        User user = UserServiceUtil.getCurrentUser();
        PermissionChecker permissionChecker = PermissionCheckerFactoryUtil.create(user);

        return ModelResourcePermissionUtil.contains(
                folderModelResourcePermission,
                permissionChecker,
                groupId,
                folderId,
                actionId
        );
    }

    public static boolean checkFileEntryPermissionsForUser(final FileEntry fileEntry, final String actionId,
                                                           final long userId) {
        User user;
        try {
            user = UserLocalServiceUtil.getUser(userId);
        } catch (PortalException e) {
            return false;
        }

        if (user == null) {
            return false;
        }

        PermissionChecker checker = permissionCheckerFactory.create(user);

        try {
            return fileEntry.containsPermission(checker, actionId);
        } catch (PortalException e) {
            return false;
        }
    }
}

