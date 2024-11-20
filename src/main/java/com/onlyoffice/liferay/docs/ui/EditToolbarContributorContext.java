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

import com.liferay.document.library.kernel.model.DLFolder;
import com.liferay.document.library.portlet.toolbar.contributor.DLPortletToolbarContributorContext;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.portlet.LiferayPortletURL;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.resource.ModelResourcePermission;
import com.liferay.portal.kernel.security.permission.resource.ModelResourcePermissionHelper;
import com.liferay.portal.kernel.servlet.taglib.ui.MenuItem;
import com.liferay.portal.kernel.servlet.taglib.ui.URLMenuItem;
import com.liferay.portal.kernel.theme.PortletDisplay;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.List;
import java.util.ResourceBundle;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;

@Component(immediate = true, service = { DLPortletToolbarContributorContext.class })
public class EditToolbarContributorContext implements DLPortletToolbarContributorContext {

	public EditToolbarContributorContext() {
	}

	public void updatePortletTitleMenuItems(final List<MenuItem> menuItems, final Folder folder,
											final ThemeDisplay themeDisplay, final PortletRequest portletRequest,
											final PortletResponse portletResponse) {
		try {
			long folderId = folder != null ? folder.getFolderId() : 0L;
			Boolean hasPermission = ModelResourcePermissionHelper.contains(_dlFolderModelResourcePermission,
					themeDisplay.getPermissionChecker(), themeDisplay.getScopeGroupId(), folderId, ActionKeys.ADD_DOCUMENT);

			if (hasPermission) {
				Layout layout = themeDisplay.getLayout();
				PortletDisplay portletDisplay = themeDisplay.getPortletDisplay();

				LiferayPortletURL portletURL;
				if (layout != null) {
					portletURL = PortletURLFactoryUtil.create(portletRequest, portletDisplay.getId(), layout,
							PortletRequest.RENDER_PHASE);
				} else {
					portletURL = PortletURLFactoryUtil.create(portletRequest, portletDisplay.getId(),
							themeDisplay.getPlid(), PortletRequest.RENDER_PHASE);
				}

				portletURL.setParameter("mvcRenderCommandName", "/document_library/create_onlyoffice");
				portletURL.setParameter("redirect", PortalUtil.getCurrentURL(portletRequest));
				
				if (folder != null) {
					portletURL.setParameter("folderId", String.valueOf(folder.getFolderId()));
				}

				String labelMenu = _translate(portletRequest, "onlyoffice-context-action-create");

				menuItems.add(this.getNewMenuItem("#create-document-onlyoffice", labelMenu, "documents-and-media", portletURL.toString()));
			}
		} catch (PortalException e) {
			_log.error(e);
		}
	}

	protected URLMenuItem getNewMenuItem(final String key, final String labelMenu, final String icon,
										 final String url) throws PortalException {
		URLMenuItem menuItem = new URLMenuItem();
		menuItem.setKey(key);
		menuItem.setLabel(labelMenu);
		menuItem.setIcon(icon);
		menuItem.setURL(url);
		return menuItem;
	}
	
	private String _translate(final PortletRequest portletRequest, final String key) {
		ResourceBundle resourceBundle = ResourceBundleUtil.getBundle(
			_portal.getLocale(portletRequest),
			EditToolbarContributorContext.class
		);

		return _language.get(resourceBundle, key);
	}

	private static final Log _log = LogFactoryUtil.getLog(EditToolbarContributorContext.class);

	@Reference(target = "(model.class.name=com.liferay.document.library.kernel.model.DLFolder)")
	private ModelResourcePermission<DLFolder> _dlFolderModelResourcePermission;
	
	@Reference
	private Language _language;

	@Reference
	private Portal _portal;

}
