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

package com.onlyoffice.liferay.docs.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.document.library.kernel.exception.NoSuchFileVersionException;
import com.liferay.document.library.kernel.model.DLVersionNumberIncrease;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.lock.Lock;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.onlyoffice.liferay.docs.model.EditingMeta;
import com.onlyoffice.manager.request.RequestManager;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.HttpEntity;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.List;

@Component(service = FileEntryUtils.class)
public final class FileEntryUtils {
    public static final long LOCKING_TIME = 60 * 1000;
    public static final String EDITOR_LOCK_OWNER = "onlyoffice-docs";
    public static final int EDITING_HASH_LENGTH = 16;

    @Reference
    private DLAppService dlAppService;
    @Reference
    private RequestManager requestManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public FileEntry createFileEntryFromUrl(final String fileName, final long repositoryId, final long folderId,
                                            final String fileUrl) throws Exception {
        return requestManager.executeGetRequest(fileUrl, new RequestManager.Callback<FileEntry>() {
            @Override
            public FileEntry doWork(final Object response) throws Exception {
                byte[] bytes = IOUtils.toByteArray(((HttpEntity) response).getContent());
                InputStream inputStream = new ByteArrayInputStream(bytes);

                return dlAppService.addFileEntry(
                        null,
                        repositoryId,
                        folderId,
                        fileName,
                        MimeTypesUtil.getContentType(fileName),
                        fileName,
                        fileName,
                        "",
                        "ONLYOFFICE Convert",
                        inputStream,
                        bytes.length,
                        null,
                        null,
                        null,
                        ServiceContextThreadLocal.getServiceContext()
                );
            }
        });
    }

    public FileEntry updateFileEntryFromUrl(final FileEntry fileEntry, final String fileUrl,
                                       final DLVersionNumberIncrease dlVersionNumberIncrease) throws Exception {
        return requestManager.executeGetRequest(fileUrl, new RequestManager.Callback<FileEntry>() {
            @Override
            public FileEntry doWork(final Object response) throws Exception {
                byte[] bytes = IOUtils.toByteArray(((HttpEntity) response).getContent());
                InputStream inputStream = new ByteArrayInputStream(bytes);

                return dlAppService.updateFileEntry(
                        fileEntry.getFileEntryId(),
                        fileEntry.getFileName(),
                        fileEntry.getMimeType(),
                        fileEntry.getTitle(),
                        fileEntry.getTitle(),
                        fileEntry.getDescription(),
                        "",
                        dlVersionNumberIncrease,
                        inputStream,
                        bytes.length,
                        null,
                        null,
                        null,
                        ServiceContextThreadLocal.getServiceContext()
                );
            }
        });
    }

    public FileVersion getFileVersion(final FileEntry fileEntry, final String version)
            throws NoSuchFileVersionException {
        List<FileVersion> fileVersions = fileEntry.getFileVersions(WorkflowConstants.STATUS_ANY);
        return fileVersions.stream()
                .filter(value -> value.getVersion().equals(version))
                .findFirst()
                .orElseThrow(() -> new NoSuchFileVersionException(
                        MessageFormat.format(
                                "No FileVersion exists with the key (version={0})",
                                version
                        )
                ));
    }

    public boolean isLockedInEditor(final FileEntry fileEntry) throws PortalException {
        Lock lock = fileEntry.getLock();

        return lock != null && lock.getOwner().startsWith(EDITOR_LOCK_OWNER);
    }

    public boolean isLockedNotInEditor(final FileEntry fileEntry) throws PortalException {
        Lock lock = fileEntry.getLock();

        return lock != null && !lock.getOwner().startsWith(EDITOR_LOCK_OWNER);
    }

    public EditingMeta getEditingMeta(final String owner) {
        if (owner == null) {
            return null;
        }

        if (!owner.startsWith(EDITOR_LOCK_OWNER)) {
            return null;
        }

        String metaString = owner.substring(EDITOR_LOCK_OWNER.length() + 1);

        try {
            return objectMapper.readValue(metaString, EditingMeta.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public String createEditorLockOwner(final String editingKey) throws JsonProcessingException {
        return MessageFormat.format(
                "{0}.{1}",
                EDITOR_LOCK_OWNER,
                objectMapper.writeValueAsString(
                        new EditingMeta(editingKey)
                )
        );
    }

    public String getEditingKey(final FileEntry fileEntry) {
        Lock lock = fileEntry.getLock();

        if (lock == null) {
            return null;
        }

        return getEditingMeta(lock.getOwner()).getEditingKey();
    }

    public String generateEditingKey(final FileEntry fileEntry) {
        return MessageFormat.format(
                "{0}_{1}",
                fileEntry.getUuid(),
                SecurityUtils.generateSecret(EDITING_HASH_LENGTH)
        );
    }

    public String formatFileId(final long groupId, final String uuid) {
        return MessageFormat.format("{0}_{1}", String.valueOf(groupId), uuid);
    }

    public FileEntryKeys deformatFileId(final String fileId) {
        String[] keys = fileId.split("_");

        return new FileEntryKeys(Long.parseLong(keys[0]), keys[1]);
    }

    public static class FileEntryKeys {
        private final String uuid;
        private final long groupId;

        public FileEntryKeys(final long groupId, final String uuid) {
            this.uuid = uuid;
            this.groupId = groupId;
        }

        public String getUuid() {
            return uuid;
        }

        public long getGroupId() {
            return groupId;
        }
    }
}
