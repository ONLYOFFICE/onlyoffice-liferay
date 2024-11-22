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

package com.onlyoffice.liferay.docs.sdk.managers;

import com.liferay.document.library.constants.DLPortletKeys;
import com.liferay.document.library.kernel.model.DLFolderConstants;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.util.PortalUtil;
import com.onlyoffice.liferay.docs.OnlyOfficeHasher;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.DefaultUrlManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.model.settings.SettingsConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.portlet.PortletRequest;
import javax.portlet.PortletURL;
import javax.ws.rs.core.UriBuilder;

@Component(
        service = UrlManager.class
)
public class UrlManagerImpl extends DefaultUrlManager {
    @Reference
    private OnlyOfficeHasher hasher;
    @Reference
    private DLAppService dlAppService;

    public UrlManagerImpl() {
        super(null);
    }

    @Reference(service = SettingsManager.class, unbind = "-")
    public void setSettingsManager(final SettingsManager settingsManager) {
        super.setSettingsManager(settingsManager);
    }

    @Override
    public String getFileUrl(final String fileId) {
        try {
            FileVersion fileVersion = dlAppService.getFileVersion(Long.parseLong(fileId));

            return UriBuilder.fromUri(getLiferayBaseUrl(false))
                    .path("/o/onlyoffice-docs/download/{groupId}/{uuid}")
                    .build(fileVersion.getGroupId(), fileVersion.getUuid())
                    .toString();
        } catch (PortalException e) {
            return null;
        }
    }

    @Override
    public String getCallbackUrl(final String fileId) {
        FileVersion fileVersion;
        Long fileEntryId;

        try {
            fileVersion = dlAppService.getFileVersion(Long.parseLong(fileId));
            fileEntryId = fileVersion.getFileEntryId();
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        } catch (PortalException e) {
            throw new RuntimeException(e);
        }

        return getLiferayBaseUrl(false) + "/o/onlyoffice/doc?key=" + hasher.getHash(fileEntryId);
    }

    @Override
    public String getGobackUrl(final String fileId) {
        ServiceContext serviceContext = ServiceContextThreadLocal.getServiceContext();

        FileVersion fileVersion;
        FileEntry fileEntry;
        try {
            fileVersion = dlAppService.getFileVersion(Long.parseLong(fileId));
            fileEntry = fileVersion.getFileEntry();
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        } catch (PortalException e) {
            throw new RuntimeException(e);
        }

        Group group = GroupLocalServiceUtil.fetchGroup(fileEntry.getGroupId());

        PortletURL portletURL = PortalUtil.getControlPanelPortletURL(
                serviceContext.getRequest(), group, DLPortletKeys.DOCUMENT_LIBRARY_ADMIN,
            0, 0, PortletRequest.RENDER_PHASE);

        long folderId = fileEntry.getFolderId();

        if (folderId == DLFolderConstants.DEFAULT_PARENT_FOLDER_ID) {
            portletURL.setParameter("mvcRenderCommandName", "/document_library/view");
        } else {
            portletURL.setParameter("mvcRenderCommandName", "/document_library/view_folder");
            portletURL.setParameter("folderId", String.valueOf(folderId));
        }

        return portletURL.toString();
    }

    private String getLiferayBaseUrl(final Boolean inner) {
        ServiceContext serviceContext = ServiceContextThreadLocal.getServiceContext();

        String productInnerUrl = getSettingsManager().getSetting(SettingsConstants.PRODUCT_INNER_URL);

        if (inner && productInnerUrl != null && !productInnerUrl.isEmpty()) {
            return sanitizeUrl(productInnerUrl);
        } else {
            return sanitizeUrl(serviceContext.getPortalURL());
        }
    }
}
