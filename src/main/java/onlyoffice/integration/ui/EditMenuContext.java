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
import com.liferay.portal.kernel.util.WebKeys;

import onlyoffice.integration.OnlyOfficeConvertUtils;
import onlyoffice.integration.OnlyOfficeUtils;

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
        } catch (PortalException e) { }

        String ext = fileVersion.getExtension();
        _canEdit = utils.isEditable(ext) && editPerm;
        _canView = utils.isViewable(ext) && viewPerm;
        _canConvert = convertUtils.isConvertable(ext) && convPerm;
    }

    public Menu getMenu() throws PortalException {
        Menu menu = super.getMenu();
        List<MenuItem> list = menu.getMenuItems();

        if (showAction()) {
            if (_canView) {
                URLMenuItem item = new URLMenuItem();
                InitViewItem(item);
                list.add(item);
            }
            if (_canConvert) {
                JavaScriptMenuItem item = new JavaScriptMenuItem();
                InitConvertItem(item);
                list.add(item);
            }
        }

        return menu;
    }

    @Override
    public List<ToolbarItem> getToolbarItems() throws PortalException {
        List<ToolbarItem> toolbarItems = super.getToolbarItems();

        if (_canView) {
            URLToolbarItem item = new URLToolbarItem();
            InitViewItem(item);
            toolbarItems.add(item);
        }
        if (_canConvert) {
            JavaScriptToolbarItem item = new JavaScriptToolbarItem();
            InitConvertItem(item);
            toolbarItems.add(item);
        }
        return toolbarItems;
    }

    private void InitViewItem(URLUIItem item) {
        item.setLabel(_canEdit ? LanguageUtil.get(request, _res, "onlyoffice-context-action-edit")
                : LanguageUtil.get(request, _res, "onlyoffice-context-action-view"));
        item.setTarget("_blank");
        item.setURL(getDocUrl());
    }

    private void InitConvertItem(JavaScriptUIItem item) {
        String lang = LanguageUtil.get(request, _res, "onlyoffice-context-action-convert");
        item.setLabel(lang);

        StringBuilder sb = new StringBuilder();

        sb.append("Liferay.Util.openWindow({");
        sb.append("dialog: {cache:false,width:500,height:200,modal:true},");
        sb.append("title: '" + lang + "',id: ");
        sb.append("'onlyofficeConvertPopup',uri:'");
        sb.append(getConvertUrl() + "'});");

        item.setOnClick(sb.toString());
    }

    private String getDocUrl() {
        PortletURL portletURL = PortletURLFactoryUtil.create(
            request, "onlyoffice_integration_ui_EditActionPortlet",
            _themeDisplay.getPlid(), PortletRequest.RENDER_PHASE);

        MutableRenderParameters params = portletURL.getRenderParameters();
        params.setValue("fileId", Long.toString(fileVersion.getFileVersionId()));

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
        params.setValue("fileId", Long.toString(fileVersion.getFileVersionId()));

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

        Settings settings = SettingsFactoryUtil.getSettings(
            new PortletInstanceSettingsLocator(
                _themeDisplay.getLayout(), portletDisplay.getId()));

        TypedSettings typedSettings = new TypedSettings(settings);

        return typedSettings.getBooleanValue("showActions");
    }

    private static final Log _log = LogFactoryUtil.getLog(
        EditMenuContext.class);

    private static final ResourceBundle _res = ResourceBundle.getBundle("content/Language");

    private ThemeDisplay _themeDisplay;
    boolean _canEdit;
    boolean _canView;
    boolean _canConvert;
}
