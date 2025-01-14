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

package com.onlyoffice.liferay.docs.sdk.managers;

import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.lock.Lock;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.onlyoffice.liferay.docs.model.EditingMeta;
import com.onlyoffice.liferay.docs.utils.EditorLockManager;
import com.onlyoffice.manager.document.DefaultDocumentManager;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.settings.SettingsManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.text.MessageFormat;

@Component(
        service = DocumentManager.class
)
public class DocumentManagerImpl extends DefaultDocumentManager {
    @Reference
    private DLAppService dlAppService;
    @Reference
    private EditorLockManager editorLockManager;

    public DocumentManagerImpl() {
        super(null);
    }

    @Reference(service = SettingsManager.class, unbind = "-")
    public void setSettingsManager(final SettingsManager settingsManager) {
        super.setSettingsManager(settingsManager);
    }

    @Override
    public String getDocumentKey(final String fileId, final boolean embedded) {
        try {
            FileVersion fileVersion = dlAppService.getFileVersion(Long.parseLong(fileId));
            FileEntry fileEntry = fileVersion.getFileEntry();

            if (embedded) {
                return MessageFormat.format(
                        "{0}_{1}",
                        fileVersion.getUuid(),
                        String.valueOf(fileVersion.getModifiedDate().getTime())
                );
            } else {
                if (editorLockManager.isLockedInEditor(fileEntry)) {
                    Lock lock = fileEntry.getLock();
                    String editingMetaAsString = lock.getOwner();

                    EditingMeta editingMeta = editorLockManager.parserEditingMeta(editingMetaAsString);
                    return editingMeta.getEditingKey();
                } else {
                    return MessageFormat.format(
                            "{0}_{1}",
                            fileVersion.getUuid(),
                            String.valueOf(fileVersion.getModifiedDate().getTime())
                    );
                }
            }
        } catch (PortalException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDocumentName(final String fileId) {
        FileVersion fileVersion;
        try {
            fileVersion = dlAppService.getFileVersion(Long.parseLong(fileId));
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        } catch (PortalException e) {
            throw new RuntimeException(e);
        }

        return fileVersion.getFileName();
    }
}
