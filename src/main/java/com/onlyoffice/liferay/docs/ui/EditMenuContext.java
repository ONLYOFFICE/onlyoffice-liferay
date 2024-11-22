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

package com.onlyoffice.liferay.docs.ui;

import com.liferay.document.library.display.context.BaseDLViewFileVersionDisplayContext;
import com.liferay.document.library.display.context.DLViewFileVersionDisplayContext;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.LiferayWindowState;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.servlet.taglib.ui.JavaScriptMenuItem;
import com.liferay.portal.kernel.servlet.taglib.ui.JavaScriptToolbarItem;
import com.liferay.portal.kernel.servlet.taglib.ui.JavaScriptUIItem;
import com.liferay.portal.kernel.servlet.taglib.ui.Menu;
import com.liferay.portal.kernel.servlet.taglib.ui.MenuItem;
import com.liferay.portal.kernel.servlet.taglib.ui.ToolbarItem;
import com.liferay.portal.kernel.servlet.taglib.ui.URLMenuItem;
import com.liferay.portal.kernel.servlet.taglib.ui.URLToolbarItem;
import com.liferay.portal.kernel.servlet.taglib.ui.URLUIItem;
import com.liferay.portal.kernel.settings.PortletInstanceSettingsLocator;
import com.liferay.portal.kernel.settings.Settings;
import com.liferay.portal.kernel.settings.SettingsException;
import com.liferay.portal.kernel.settings.SettingsFactoryUtil;
import com.liferay.portal.kernel.settings.TypedSettings;
import com.liferay.portal.kernel.theme.PortletDisplay;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.PortletKeys;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.onlyoffice.manager.document.DocumentManager;

import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
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
    private boolean canFillForm;
    private boolean canView;
    private boolean canConvert;

    public EditMenuContext(final UUID uuid, final DLViewFileVersionDisplayContext parentDLDisplayContext,
                           final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse,
                           final FileVersion fileVersion, final DocumentManager documentManager,
                           final PermissionCheckerFactory permissionFactory) {

        super(
            uuid, parentDLDisplayContext, httpServletRequest,
            httpServletResponse, fileVersion);

        themeDisplay = (ThemeDisplay) httpServletRequest.getAttribute(WebKeys.THEME_DISPLAY);
        resourceBundle = ResourceBundleUtil.getBundle("content.Language",
                httpServletRequest.getLocale(), getClass());

        boolean editPerm = false;
        boolean viewPerm = false;
        boolean convPerm = false;

        try {
            PermissionChecker checker = permissionFactory.create(PortalUtil.getUser(httpServletRequest));
            FileEntry fe = fileVersion.getFileEntry();
            Folder folder = fe.getFolder();

            editPerm = fe.containsPermission(checker, ActionKeys.UPDATE);
            viewPerm = fe.containsPermission(checker, ActionKeys.VIEW);
            convPerm = folder.containsPermission(checker, ActionKeys.ADD_DOCUMENT) && viewPerm;
        } catch (PortalException e) {
            // Do nothing if exception
        }

        String fileName = fileVersion.getFileName();
        canEdit = documentManager.isEditable(fileName) && editPerm;
        canFillForm = documentManager.isFillable(fileName) && editPerm;
        canView = documentManager.isViewable(fileName) && viewPerm;
        canConvert = documentManager.getDefaultConvertExtension(fileName) != null && convPerm;
    }

    public Menu getMenu() throws PortalException {
        Menu menu = super.getMenu();
        List<MenuItem> list = menu.getMenuItems();

        if (showAction()) {
            if (canView) {
                URLMenuItem item = new URLMenuItem();
                initViewItem(item);
                list.add(item);
            }
            if (canConvert) {
                JavaScriptMenuItem item = new JavaScriptMenuItem();
                initConvertItem(item);
                list.add(item);
            }
        }

        return menu;
    }

    @Override
    public List<ToolbarItem> getToolbarItems() throws PortalException {
        List<ToolbarItem> toolbarItems = super.getToolbarItems();

        if (canView) {
            URLToolbarItem item = new URLToolbarItem();
            initViewItem(item);
            toolbarItems.add(item);
        }
        if (canConvert) {
            JavaScriptToolbarItem item = new JavaScriptToolbarItem();
            initConvertItem(item);
            toolbarItems.add(item);
        }
        return toolbarItems;
    }

    private void initViewItem(final URLUIItem item) {
        String labelKey = "onlyoffice-context-action-view";

        if (canEdit) {
            labelKey = "onlyoffice-context-action-edit";
        } else if (canFillForm)  {
            labelKey = "onlyoffice-context-action-fillForm";
        }

        item.setLabel(LanguageUtil.get(request, resourceBundle, labelKey));
        item.setTarget("_blank");
        item.setURL(getDocUrl());
    }

    private void initConvertItem(final JavaScriptUIItem item) {
        String label = LanguageUtil.get(request, resourceBundle, "onlyoffice-context-action-convert");

        StringBuilder sb = new StringBuilder();

        sb.append("Liferay.Util.openWindow({");
        sb.append("dialog: {destroyOnHide:true,cache:false,width:500,height:200,modal:true,resizable: false},");
        sb.append("title: '" + label + "',id: ");
        sb.append("'onlyofficeConvertPopup',uri:'");
        sb.append(getConvertUrl() + "'});");

        item.setLabel(label);
        item.setOnClick(sb.toString());
    }

    private String getDocUrl() {
        PortletURL portletURL = PortletURLFactoryUtil.create(
                request,
                com.onlyoffice.liferay.docs.constants.PortletKeys.EDITOR,
                themeDisplay.getPlid(),
                PortletRequest.RENDER_PHASE
        );

//      MutableRenderParameters added in portlet version 3.0
//      MutableRenderParameters params = portletURL.getRenderParameters();
//      params.setValue("fileId", Long.toString(fileVersion.getFileVersionId()));

        portletURL.setParameter("fileEntryId", Long.toString(fileVersion.getFileEntryId()));

        try {
            portletURL.setWindowState(LiferayWindowState.EXCLUSIVE);
        } catch (WindowStateException wse) {
            log.error(wse.getMessage(), wse);
        }

        return portletURL.toString();
    }

    private String getConvertUrl() {
        PortletURL portletURL = PortletURLFactoryUtil.create(
            request, "com_onlyoffice_liferay_docs_ui_ConvertActionPortlet",
            themeDisplay.getPlid(), PortletRequest.RENDER_PHASE);

//      MutableRenderParameters added in portlet version 3.0
//      MutableRenderParameters params = portletURL.getRenderParameters();
//      params.setValue("fileId", Long.toString(fileVersion.getFileVersionId()));

        portletURL.setParameter("fileId", String.valueOf(fileVersion.getFileEntryId()));

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

        Settings settings = SettingsFactoryUtil.getSettings(
            new PortletInstanceSettingsLocator(
                themeDisplay.getLayout(), portletDisplay.getId()));

        TypedSettings typedSettings = new TypedSettings(settings);

        return typedSettings.getBooleanValue("showActions");
    }
}
