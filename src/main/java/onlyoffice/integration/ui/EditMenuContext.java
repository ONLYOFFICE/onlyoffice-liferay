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
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.servlet.taglib.ui.Menu;
import com.liferay.portal.kernel.servlet.taglib.ui.MenuItem;
import com.liferay.portal.kernel.servlet.taglib.ui.URLMenuItem;
import com.liferay.portal.kernel.settings.PortletInstanceSettingsLocator;
import com.liferay.portal.kernel.settings.Settings;
import com.liferay.portal.kernel.settings.SettingsException;
import com.liferay.portal.kernel.settings.SettingsFactoryUtil;
import com.liferay.portal.kernel.settings.TypedSettings;
import com.liferay.portal.kernel.theme.PortletDisplay;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.PortletKeys;
import com.liferay.portal.kernel.util.WebKeys;

import onlyoffice.integration.OnlyOfficeUtils;

public class EditMenuContext
extends BaseDLViewFileVersionDisplayContext {
	
	public EditMenuContext(
		UUID uuid, DLViewFileVersionDisplayContext parentDLDisplayContext,
		HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse, FileVersion fileVersion, OnlyOfficeUtils utils) {
	
		super(
			uuid, parentDLDisplayContext, httpServletRequest,
			httpServletResponse, fileVersion);
	
		_themeDisplay = (ThemeDisplay)httpServletRequest.getAttribute(
			WebKeys.THEME_DISPLAY);
		_utils = utils;
	}
	
	public Menu getMenu() throws PortalException {
		Menu menu = super.getMenu();
	
		String ext = fileVersion.getExtension();
		boolean edit = _utils.isEditable(ext);
		boolean view = _utils.isViewable(ext);
		
		if (showAction() && (edit || view)) {
			URLMenuItem item = new URLMenuItem();
			
			item.setLabel(edit ? LanguageUtil.get(request, _res, "onlyoffice-context-action-edit")
					: LanguageUtil.get(request, _res, "onlyoffice-context-action-view"));
			item.setTarget("_blank");
			item.setURL(getDocUrl());
	
			List<MenuItem> list = menu.getMenuItems();
	
			list.add(item);
		}
	
		return menu;
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
	private OnlyOfficeUtils _utils;
}
