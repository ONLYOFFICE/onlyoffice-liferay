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

package com.onlyoffice.liferay.docs.permission;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.security.permission.resource.ModelResourcePermission;
import com.liferay.portal.kernel.security.permission.resource.ModelResourcePermissionHelper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = {})
public class OnlyOfficePermissionUtils {
    private static ModelResourcePermission<Folder> folderModelResourcePermission;

    public static boolean saveAs(final FileEntry file, final User user) throws PortalException {
        PermissionChecker checker = PermissionCheckerFactoryUtil.create(user);

        return file.containsPermission(checker, ActionKeys.VIEW)
                && ModelResourcePermissionHelper.contains(
                        folderModelResourcePermission,checker, file.getGroupId(),
                        file.getFolderId(), ActionKeys.ADD_DOCUMENT);
    }

    @Reference(
        target = "(model.class.name=com.liferay.portal.kernel.repository.model.Folder)",
        unbind = "-"
    )
    protected void setFolderModelResourcePermission(final ModelResourcePermission<Folder> modelResourcePermission) {
        folderModelResourcePermission = modelResourcePermission;
    }
}
