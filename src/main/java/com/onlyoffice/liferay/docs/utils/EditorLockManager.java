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

package com.onlyoffice.liferay.docs.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.lock.Lock;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.transaction.TransactionConfig;
import com.liferay.portal.kernel.transaction.TransactionInvokerUtil;
import com.onlyoffice.liferay.docs.model.EditingMeta;
import lombok.SneakyThrows;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.text.MessageFormat;


@Component(service = EditorLockManager.class)
public class EditorLockManager {
    public static final int TIMEOUT_INFINITY = 0;
    public static final int TIMEOUT_CONNECTING_EDITOR = 60 * 1000;

    public static final String EDITING_META_PREFIX = "onlyoffice-docs";
    public static final int EDITING_HASH_LENGTH = 16;

    @Reference
    private DLAppService dlAppService;

    private final ObjectMapper objectMapper = new ObjectMapper();


    public void lockInEditor(final FileEntry fileEntry, final int expirationTime) {
        String editingKey = generateEditingKey(fileEntry);
        EditingMeta editingMeta = new EditingMeta(editingKey);

        lockInEditor(fileEntry, editingMeta, expirationTime);
    }

    public void lockInEditor(final FileEntry fileEntry, final String editingMetaAsString) {
        lockInEditor(fileEntry, editingMetaAsString, TIMEOUT_INFINITY);
    }

    public void lockInEditor(final FileEntry fileEntry, final EditingMeta editingMeta, final int expirationTime) {
        try {
            String editingMetaAsString = MessageFormat.format(
                    "{0}.{1}",
                    EDITING_META_PREFIX,
                    objectMapper.writeValueAsString(editingMeta)
            );

            lockInEditor(fileEntry, editingMetaAsString, expirationTime);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    public void lockInEditor(final FileEntry fileEntry, final String editingMetaAsString, final int expirationTime) {
        TransactionInvokerUtil.invoke(
                TransactionConfig.Factory.create(
                        Propagation.REQUIRED,
                        new Class<?>[] {Exception.class}
                ),
                () -> {
                    ServiceContext serviceContext = ServiceContextThreadLocal.getServiceContext();

                    FileEntry checkOutFileEntry = dlAppService.checkOutFileEntry(
                            fileEntry.getFileEntryId(),
                            editingMetaAsString,
                            expirationTime,
                            serviceContext
                    );

                    // dlAppService.checkOutFileEntry with expirationTime equals 0, lock file entry only on 1 hour
                    if (expirationTime == TIMEOUT_INFINITY) {
                        dlAppService.refreshFileEntryLock(
                                checkOutFileEntry.getLock().getUuid(),
                                checkOutFileEntry.getLock().getCompanyId(),
                                TIMEOUT_INFINITY
                        );
                    }

                    return null;
                });
    }

    public void unlockFromEditor(final FileEntry fileEntry) throws PortalException {
        Lock lock = fileEntry.getLock();

        SecurityUtils.runAs(new SecurityUtils.RunAsWork<Void>() {
            public Void doWork() throws PortalException {
                dlAppService.cancelCheckOut(fileEntry.getFileEntryId());
                return null;
            }
        }, lock.getUserId());
    }

    public boolean isLockedInEditor(final FileEntry fileEntry) {
        Lock lock = fileEntry.getLock();
        if (lock == null) {
            return false;
        }

        String editingMetaAsString = lock.getOwner();

        return editingMetaAsString != null && editingMetaAsString.startsWith(EDITING_META_PREFIX);
    }

    public boolean isLockedNotInEditor(final FileEntry fileEntry) {
        Lock lock = fileEntry.getLock();
        if (lock == null) {
            return false;
        }

        String editingMetaAsString = lock.getOwner();

        return editingMetaAsString == null || !editingMetaAsString.startsWith(EDITING_META_PREFIX);
    }

    public boolean isValidDocumentKey(final FileEntry fileEntry, final String key) {
        Lock lock = fileEntry.getLock();
        if (lock == null) {
            return false;
        }

        String editingMetaAsString = lock.getOwner();
        EditingMeta editingMeta = parserEditingMeta(editingMetaAsString);

        String currentKey = editingMeta.getEditingKey();
        if (currentKey == null || currentKey.isEmpty()) {
            return false;
        }

        return currentKey.equals(key);
    }

    @SneakyThrows
    public void changeLockOwner(final FileEntry fileEntry, final long newLockOwner) throws PortalException {
        Lock lock = fileEntry.getLock();
        String editingMetaAsString = lock.getOwner();

        TransactionInvokerUtil.invoke(
                TransactionConfig.Factory.create(
                        Propagation.REQUIRED,
                        new Class<?>[] {Exception.class}
                ),
                () -> {
                    unlockFromEditor(fileEntry);

                    SecurityUtils.runAs(new SecurityUtils.RunAsWork<Void>() {
                        public Void doWork() throws PortalException {
                            lockInEditor(fileEntry, editingMetaAsString);
                            return null;
                        }
                    }, newLockOwner);

                    return null;
                });
    }

    @SneakyThrows
    public void refreshTimeToExpireLock(final FileEntry fileEntry, final int expirationTime) throws PortalException {
        Lock lock = fileEntry.getLock();
        String editingMetaAsString = lock.getOwner();

        TransactionInvokerUtil.invoke(
                TransactionConfig.Factory.create(
                        Propagation.REQUIRED,
                        new Class<?>[] {Exception.class}
                ),
                () -> {

                    unlockFromEditor(fileEntry);

                    lockInEditor(fileEntry, editingMetaAsString, expirationTime);

                    return null;
                });
    }

    public EditingMeta parserEditingMeta(final String editingMetaAsString) {
        if (editingMetaAsString == null) {
            return null;
        }

        if (!editingMetaAsString.startsWith(EDITING_META_PREFIX)) {
            return null;
        }

        String metaString = editingMetaAsString.substring(EDITING_META_PREFIX.length() + 1);

        try {
            return objectMapper.readValue(metaString, EditingMeta.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String generateEditingKey(final FileEntry fileEntry) {
        return MessageFormat.format(
                "{0}_{1}",
                fileEntry.getUuid(),
                SecurityUtils.generateSecret(EDITING_HASH_LENGTH)
        );
    }
}
