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

package com.onlyoffice.liferay.docs.ui;

import com.liferay.document.library.display.context.BaseDLViewFileVersionDisplayContext;
import com.liferay.document.library.display.context.DLViewFileVersionDisplayContext;
import com.liferay.frontend.taglib.clay.servlet.taglib.util.DropdownItem;
import com.liferay.frontend.taglib.clay.servlet.taglib.util.DropdownItemBuilder;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.portlet.LiferayWindowState;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.settings.FallbackKeysSettingsUtil;
import com.liferay.portal.kernel.settings.PortletInstanceSettingsLocator;
import com.liferay.portal.kernel.settings.Settings;
import com.liferay.portal.kernel.settings.SettingsException;
import com.liferay.portal.kernel.settings.TypedSettings;
import com.liferay.portal.kernel.theme.PortletDisplay;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.PortletKeys;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.onlyoffice.liferay.docs.utils.EditorLockManager;
import com.onlyoffice.liferay.docs.utils.PermissionUtils;
import com.onlyoffice.manager.document.DocumentManager;

import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import javax.portlet.MutableRenderParameters;
import javax.portlet.PortletRequest;
import javax.portlet.PortletURL;
import javax.portlet.WindowStateException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EditMenuContext extends BaseDLViewFileVersionDisplayContext {
    private static final Log log = LogFactoryUtil.getLog(EditMenuContext.class);

    private ThemeDisplay themeDisplay;
    private ResourceBundle resourceBundle;
    private boolean canEdit;
    private boolean canView;
    private boolean canConvert;

    public EditMenuContext(final UUID uuid, final DLViewFileVersionDisplayContext parentDLDisplayContext,
                           final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse,
                           final FileVersion fileVersion, final DocumentManager documentManager,
                           final EditorLockManager editorLockManager,
                           final PermissionCheckerFactory permissionFactory) {

        super(
            uuid, parentDLDisplayContext, httpServletRequest,
            httpServletResponse, fileVersion);

        themeDisplay = (ThemeDisplay) httpServletRequest.getAttribute(WebKeys.THEME_DISPLAY);
        resourceBundle = ResourceBundleUtil.getBundle(
                "content.Language",
                themeDisplay.getLocale(),
                getClass()
        );

        boolean editPerm = false;
        boolean viewPerm = false;
        boolean convPerm = false;

        try {
            User user = PortalUtil.getUser(httpServletRequest);
            PermissionChecker checker = permissionFactory.create(user);
            FileEntry fileEntry = fileVersion.getFileEntry();
            Folder folder = fileEntry.getFolder();

            editPerm = fileEntry.containsPermission(checker, ActionKeys.UPDATE)
                    && !editorLockManager.isLockedNotInEditor(fileEntry);
            viewPerm = fileEntry.containsPermission(checker, ActionKeys.VIEW);
            convPerm = PermissionUtils.checkFolderPermission(
                    themeDisplay.getScopeGroupId(),
                    folder.getFolderId(),
                    ActionKeys.ADD_DOCUMENT
            ) && viewPerm;
        } catch (PortalException e) {
            // Do nothing if exception
        }

        String fileName = fileVersion.getFileName();
        canEdit = documentManager.isEditable(fileName) && editPerm;
        canView = documentManager.isViewable(fileName) && viewPerm;
        canConvert = documentManager.getDefaultConvertExtension(fileName) != null && convPerm;
    }

    public List<DropdownItem> getActionDropdownItems() throws PortalException {
        List<DropdownItem> dropdownItems = super.getActionDropdownItems();

        if (showAction()) {
            if (canView) {
                dropdownItems.add(createViewDropdownItem());
            }
            if (canConvert) {
                dropdownItems.add(createConvertDropdownItem());
            }
        }

        return dropdownItems;
    }


    private DropdownItem createViewDropdownItem() {
        String labelKey = "onlyoffice-context-action-view";
        String iconKey = "view";

        if (canEdit) {
            labelKey = "onlyoffice-context-action-edit";
            iconKey = "pencil";
        }

        return DropdownItemBuilder.setHref(getDocUrl())
                .setIcon(iconKey)
                .setLabel(LanguageUtil.get(request, resourceBundle, labelKey))
                .setTarget("_blank")
                .build();
    }

    private DropdownItem createConvertDropdownItem() {
        String label = LanguageUtil.get(request, resourceBundle, "onlyoffice-context-action-convert");

        StringBuilder sb = new StringBuilder();

        sb.append("Liferay.Util.openWindow({");
        sb.append("dialog: {destroyOnHide:true,cache:false,width:500,height:200,modal:true,resizable: false},");
        sb.append("title: '" + label + "',id: ");
        sb.append("'onlyofficeConvertPopup',uri:'");
        sb.append(getConvertUrl() + "'});");

        return DropdownItemBuilder.setHref("javascript:" + sb)
                .setIcon("change")
                .setLabel(label)
                .build();
    }

    private String getDocUrl() {
        PortletURL portletURL = PortletURLFactoryUtil.create(
                request,
                com.onlyoffice.liferay.docs.constants.PortletKeys.EDITOR,
                themeDisplay.getPlid(),
                PortletRequest.RENDER_PHASE
        );

        MutableRenderParameters params = portletURL.getRenderParameters();
        params.setValue("fileEntryId", Long.toString(fileVersion.getFileEntryId()));

        try {
            portletURL.setWindowState(LiferayWindowState.EXCLUSIVE);
        } catch (WindowStateException wse) {
            log.error(wse.getMessage(), wse);
        }

        return portletURL.toString();
    }

    private String getConvertUrl() {
        PortletURL portletURL = PortletURLFactoryUtil.create(
                request,
                com.onlyoffice.liferay.docs.constants.PortletKeys.CONVERT,
                themeDisplay.getPlid(),
                PortletRequest.RENDER_PHASE
        );

        MutableRenderParameters params = portletURL.getRenderParameters();
        params.setValue("fileEntryId", Long.toString(fileVersion.getFileEntryId()));

        try {
            portletURL.setWindowState(LiferayWindowState.POP_UP);
        } catch (WindowStateException wse) {
            log.error(wse.getMessage(), wse);
        }

        return portletURL.toString();
    }

    private boolean showAction() throws SettingsException {
        PortletDisplay portletDisplay = themeDisplay.getPortletDisplay();

        String portletName = portletDisplay.getPortletName();

        if (portletName.equals(PortletKeys.DOCUMENT_LIBRARY_ADMIN)) {
            return true;
        }

        Settings settings = FallbackKeysSettingsUtil.getSettings(
            new PortletInstanceSettingsLocator(
                themeDisplay.getLayout(), portletDisplay.getId()));

        TypedSettings typedSettings = new TypedSettings(settings);

        return typedSettings.getBooleanValue("showActions");
    }
}
