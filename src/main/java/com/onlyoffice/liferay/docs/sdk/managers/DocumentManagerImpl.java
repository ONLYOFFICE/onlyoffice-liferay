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

import com.liferay.document.library.kernel.service.DLAppLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.onlyoffice.liferay.docs.utils.OnlyOfficeUtils;
import com.onlyoffice.manager.document.DefaultDocumentManager;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.settings.SettingsManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
    service = DocumentManager.class
)
public class DocumentManagerImpl extends DefaultDocumentManager {

    public DocumentManagerImpl() {
        super(null);
    }

    @Override
    public String getDocumentKey(String fileId, boolean embedded) {
        try {
            FileVersion fileVersion = DLAppLocalServiceUtil.getFileVersion(Long.parseLong(fileId));

            if (embedded && !fileVersion.getVersion().equals("PWC")) {
                return createDocKey(fileVersion, true);
            } else {
                String key = _OOUtils.getCollaborativeEditingKey(fileVersion.getFileEntry());

                if (key != null) {
                    return key;
                } else {
                    return createDocKey(fileVersion, false);
                }
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        } catch (PortalException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDocumentName(String fileId) {
        FileVersion fileVersion;
        try {
            fileVersion = DLAppLocalServiceUtil.getFileVersion(Long.parseLong(fileId));
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        } catch (PortalException e) {
            throw new RuntimeException(e);
        }

        return fileVersion.getFileName();
    }

    private String createDocKey(FileVersion fileVersion, boolean versionSpecific) throws PortalException {
        String key = fileVersion.getFileEntry().getUuid() + "_" + fileVersion.getVersion().hashCode();

        if (versionSpecific) {
            key = key + "_version";
        }

        return key;
    }

    @Reference
    private OnlyOfficeUtils _OOUtils;

    @Reference(service = SettingsManager.class, unbind = "-")
    public void setSettingsManager(
            SettingsManager settingsManager) {
        super.setSettingsManager(settingsManager);
    }

}
