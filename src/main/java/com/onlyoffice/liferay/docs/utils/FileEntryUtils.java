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

import com.liferay.document.library.kernel.model.DLVersionNumberIncrease;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.onlyoffice.manager.request.RequestManager;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.HttpEntity;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.MessageFormat;

@Component(service = FileEntryUtils.class)
public final class FileEntryUtils {
    @Reference
    private DLAppService dlAppService;
    @Reference
    private RequestManager requestManager;

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
                        fileEntry.getDescription(),
                        "",
                        dlVersionNumberIncrease,
                        inputStream,
                        bytes.length,
                        ServiceContextThreadLocal.getServiceContext()
                );
            }
        });
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
