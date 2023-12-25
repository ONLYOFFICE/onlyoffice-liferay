/**
 *
 * (c) Copyright Ascensio System SIA 2023
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

package onlyoffice.integration.ui;

import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.portlet.MutableRenderParameters;
import javax.portlet.PortletRequest;
import javax.portlet.PortletURL;
import javax.portlet.WindowStateException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.document.library.display.context.BaseDLViewFileVersionDisplayContext;
import com.liferay.document.library.display.context.DLViewFileVersionDisplayContext;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.portlet.LiferayWindowState;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.settings.PortletInstanceSettingsLocator;
import com.liferay.portal.kernel.settings.Settings;
import com.liferay.portal.kernel.settings.SettingsException;
import com.liferay.portal.kernel.settings.FallbackKeysSettingsUtil;
import com.liferay.portal.kernel.settings.TypedSettings;
import com.liferay.portal.kernel.theme.PortletDisplay;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.PortletKeys;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.frontend.taglib.clay.servlet.taglib.util.DropdownItem;
import com.liferay.frontend.taglib.clay.servlet.taglib.util.DropdownItemBuilder;

import onlyoffice.integration.OnlyOfficeConvertUtils;
import onlyoffice.integration.OnlyOfficeUtils;
import onlyoffice.integration.permission.OnlyOfficePermissionUtils;

public class EditMenuContext
extends BaseDLViewFileVersionDisplayContext {

    public EditMenuContext(
        UUID uuid, DLViewFileVersionDisplayContext parentDLDisplayContext,
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse, FileVersion fileVersion, OnlyOfficeUtils utils,  OnlyOfficeConvertUtils convertUtils,
        PermissionCheckerFactory permissionFactory) {

        super(
            uuid, parentDLDisplayContext, httpServletRequest,
            httpServletResponse, fileVersion);

        _themeDisplay = (ThemeDisplay)httpServletRequest.getAttribute(
            WebKeys.THEME_DISPLAY);
        _resourceBundle = ResourceBundleUtil.getBundle("content.Language",
                _themeDisplay.getLocale(), getClass());

        boolean editPerm = false;
        boolean viewPerm = false;
        boolean convPerm = false;

        try {
            User user = PortalUtil.getUser(httpServletRequest);
            PermissionChecker checker = permissionFactory.create(user);
            FileEntry fe = fileVersion.getFileEntry();

            editPerm = fe.containsPermission(checker, ActionKeys.UPDATE);
            viewPerm = fe.containsPermission(checker, ActionKeys.VIEW);
            convPerm = OnlyOfficePermissionUtils.create(fe.getGroupId(), fe.getFolderId(), user) && viewPerm;
        } catch (PortalException e) { }

        String ext = fileVersion.getExtension();
        _canEdit = utils.isEditable(ext) && editPerm;
        _canFillForm = utils.isFillForm(ext) && editPerm;
        _canView = utils.isViewable(ext) && viewPerm;
        _canConvert = convertUtils.isConvertable(ext) && convPerm;
        _isMasterForm = ext.equals("docxf");
    }

    public List<DropdownItem> getActionDropdownItems() throws PortalException {
        List<DropdownItem> dropdownItems = super.getActionDropdownItems();

        if (showAction()) {
            if (_canView) {
                dropdownItems.add(_createViewDropdownItem());
            }
            if (_canConvert) {
                dropdownItems.add(_createConvertDropdownItem());
            }
        }

        return dropdownItems;
    }

    private DropdownItem _createViewDropdownItem()
            throws PortalException {

        String labelKey = "onlyoffice-context-action-view";

        if (_canEdit) {
            labelKey = "onlyoffice-context-action-edit";
        } else if (_canFillForm)  {
            labelKey = "onlyoffice-context-action-fillForm";
        }

        return DropdownItemBuilder.setHref(
                    getDocUrl())
                .setLabel(LanguageUtil.get(request, _resourceBundle, labelKey))
                .setTarget("_blank")
                .build();
    }

    private DropdownItem _createConvertDropdownItem()
            throws PortalException {

        String lang = null;
        if (_isMasterForm) {
            lang = LanguageUtil.get(request, _resourceBundle, "onlyoffice-context-action-create-form");
        } else {
            lang = LanguageUtil.get(request, _resourceBundle, "onlyoffice-context-action-convert");
        }

        StringBuilder sb = new StringBuilder();

        sb.append("Liferay.Util.openWindow({");
        sb.append("dialog: {destroyOnHide:true,cache:false,width:500,height:200,modal:true,resizable: false},");
        sb.append("title: '" + lang + "',id: ");
        sb.append("'onlyofficeConvertPopup',uri:'");
        sb.append(getConvertUrl() + "'});");

        return DropdownItemBuilder.setHref("javascript:" + sb)
                .setLabel(lang)
                .build();
    }

    private String getDocUrl() {
        PortletURL portletURL = PortletURLFactoryUtil.create(
            request, "onlyoffice_integration_ui_EditActionPortlet",
            _themeDisplay.getPlid(), PortletRequest.RENDER_PHASE);

        MutableRenderParameters params = portletURL.getRenderParameters();
        params.setValue("fileId", Long.toString(fileVersion.getFileEntryId()));

        try {
            portletURL.setWindowState(LiferayWindowState.EXCLUSIVE);
        }
        catch (WindowStateException wse) {
            _log.error(wse.getMessage(), wse);
        }

        return portletURL.toString();
    }

    private String getConvertUrl() {
        PortletURL portletURL = PortletURLFactoryUtil.create(
            request, "onlyoffice_integration_ui_ConvertActionPortlet",
            _themeDisplay.getPlid(), PortletRequest.RENDER_PHASE);

        MutableRenderParameters params = portletURL.getRenderParameters();
        params.setValue("fileId", String.valueOf(fileVersion.getFileEntryId()));

        try {
            portletURL.setWindowState(LiferayWindowState.POP_UP);
        }
        catch (WindowStateException wse) {
            _log.error(wse.getMessage(), wse);
        }

        return portletURL.toString();
    }

    private boolean showAction() throws SettingsException {
        PortletDisplay portletDisplay = _themeDisplay.getPortletDisplay();

        String portletName = portletDisplay.getPortletName();

        if (portletName.equals(PortletKeys.DOCUMENT_LIBRARY_ADMIN)) {
            return true;
        }

        Settings settings = FallbackKeysSettingsUtil.getSettings(
            new PortletInstanceSettingsLocator(
                _themeDisplay.getLayout(), portletDisplay.getId()));

        TypedSettings typedSettings = new TypedSettings(settings);

        return typedSettings.getBooleanValue("showActions");
    }

    private static final Log _log = LogFactoryUtil.getLog(
        EditMenuContext.class);

    private ThemeDisplay _themeDisplay;
    private ResourceBundle _resourceBundle;
    boolean _canEdit;
    boolean _canFillForm;
    boolean _canView;
    boolean _canConvert;
    boolean _isMasterForm;
}
