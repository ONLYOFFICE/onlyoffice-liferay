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

package com.onlyoffice.liferay.docs.sdk.service;

import com.liferay.document.library.kernel.model.DLVersionNumberIncrease;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.lock.Lock;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.transaction.TransactionConfig;
import com.liferay.portal.kernel.transaction.TransactionInvokerUtil;
import com.onlyoffice.liferay.docs.model.EditingMeta;
import com.onlyoffice.liferay.docs.utils.EditorLockManager;
import com.onlyoffice.liferay.docs.utils.FileEntryUtils;
import com.onlyoffice.liferay.docs.utils.PermissionUtils;
import com.onlyoffice.liferay.docs.utils.SecurityUtils;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.model.documenteditor.callback.Action;
import com.onlyoffice.service.documenteditor.callback.CallbackService;
import com.onlyoffice.service.documenteditor.callback.DefaultCallbackService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.text.MessageFormat;
import java.util.List;


@Component(
        service = CallbackService.class
)
public class CallbackServiceImpl extends DefaultCallbackService {
    private static final Log log = LogFactoryUtil.getLog(CallbackServiceImpl.class);

    @Reference
    private DLAppService dlAppService;
    @Reference
    private UrlManager urlManager;
    @Reference
    private FileEntryUtils fileEntryUtils;
    @Reference
    private EditorLockManager editorLockManager;

    public CallbackServiceImpl() {
        super(null, null);
    }

    @Reference(service = SettingsManager.class, unbind = "-")
    public void setSettingsManager(final SettingsManager settingsManager) {
        super.setSettingsManager(settingsManager);
    }

    @Reference(service = JwtManager.class, unbind = "-")
    public void setJwtManager(final JwtManager jwtManager) {
        super.setJwtManager(jwtManager);
    }

    public void handlerEditing(final Callback callback, final String fileId) throws Exception {
        List<Action> actions = callback.getActions();
        FileEntryUtils.FileEntryKeys fileEntryKeys = fileEntryUtils.deformatFileId(fileId);

        for (Action action : actions) {
            long userId = Long.parseLong(action.getUserid());

            SecurityUtils.setUserAuthentication(userId);

            FileEntry fileEntry = dlAppService.getFileEntryByUuidAndGroupId(
                    fileEntryKeys.getUuid(),
                    fileEntryKeys.getGroupId()
            );

            switch (action.getType()) {
                case CONNECTED:
                    handlerConnecting(callback, fileEntry);
                    break;
                case DISCONNECTED:
                    handlerDisconnecting(callback, fileEntry);
                    break;
                default:
            }
        }
    }

    public void handlerSave(final Callback callback, final String fileId) throws Exception {
        String key = callback.getKey();
        String fileUrl = callback.getUrl();
        List<Action> actions = callback.getActions();
        FileEntryUtils.FileEntryKeys fileEntryKeys = fileEntryUtils.deformatFileId(fileId);

        for (Action action : actions) {
            long userId = Long.parseLong(action.getUserid());

            SecurityUtils.setUserAuthentication(userId);

            FileEntry fileEntry = dlAppService.getFileEntryByUuidAndGroupId(
                    fileEntryKeys.getUuid(),
                    fileEntryKeys.getGroupId()
            );

            if (!editorLockManager.isLockedInEditor(fileEntry)) {
                throw new RuntimeException(
                        MessageFormat.format(
                                "FileEntry with ID ({0}) is not locked in ONLYOFFICE Docs Editor",
                                String.valueOf(fileEntry.getFileEntryId())
                        )
                );
            }

            if (editorLockManager.isLockedInEditor(fileEntry)
                    && !editorLockManager.isValidDocumentKey(fileEntry, key)) {
                throw new RuntimeException(
                        MessageFormat.format(
                                "FileEntry with ID ({0}) is locked in ONLYOFFICE Docs Editor, but key ({1}) is"
                                        + "not valid",
                                String.valueOf(fileEntry.getFileEntryId()),
                                key
                        )
                );
            }

            saveFileEntry(
                    fileEntry,
                    urlManager.replaceToInnerDocumentServerUrl(fileUrl),
                    DLVersionNumberIncrease.MAJOR,
                    false
            );
        }
    }

    public void handlerClosed(final Callback callback, final String fileId) throws Exception {
        String key = callback.getKey();
        List<Action> actions = callback.getActions();
        FileEntryUtils.FileEntryKeys fileEntryKeys = fileEntryUtils.deformatFileId(fileId);

        for (Action action : actions) {
            long userId = Long.parseLong(action.getUserid());

            SecurityUtils.setUserAuthentication(userId);

            FileEntry fileEntry = dlAppService.getFileEntryByUuidAndGroupId(
                    fileEntryKeys.getUuid(),
                    fileEntryKeys.getGroupId()
            );

            if (!editorLockManager.isLockedInEditor(fileEntry)) {
                throw new RuntimeException(
                        MessageFormat.format(
                                "FileEntry with ID ({0}) is not locked in ONLYOFFICE Docs Editor",
                                String.valueOf(fileEntry.getFileEntryId())
                        )
                );
            }

            if (editorLockManager.isLockedInEditor(fileEntry)
                    && !editorLockManager.isValidDocumentKey(fileEntry, key)) {
                throw new RuntimeException(
                        MessageFormat.format(
                                "FileEntry with ID ({0}) is locked in ONLYOFFICE Docs Editor, but key ({1}) is"
                                        + "not valid",
                                String.valueOf(fileEntry.getFileEntryId()),
                                key
                        )
                );
            }

            editorLockManager.unlockFromEditor(fileEntry);
        }
    }

    public void handlerForcesave(final Callback callback, final String fileId) throws Exception {
        if (!getSettingsManager().getSettingBoolean("customization.forcesave", false)) {
            log.info("Forcesave is disabled, ignoring forcesave request");
            return;
        }

        String key = callback.getKey();
        String fileUrl = callback.getUrl();
        List<Action> actions = callback.getActions();
        FileEntryUtils.FileEntryKeys fileEntryKeys = fileEntryUtils.deformatFileId(fileId);

        for (Action action : actions) {
            long userId = Long.parseLong(action.getUserid());

            SecurityUtils.setUserAuthentication(userId);

            FileEntry fileEntry = dlAppService.getFileEntryByUuidAndGroupId(
                    fileEntryKeys.getUuid(),
                    fileEntryKeys.getGroupId()
            );

            if (!editorLockManager.isLockedInEditor(fileEntry)) {
                throw new RuntimeException(
                        MessageFormat.format(
                                "FileEntry with ID ({0}) is not locked in ONLYOFFICE Docs Editor",
                                String.valueOf(fileEntry.getFileEntryId())
                        )
                );
            }

            if (editorLockManager.isLockedInEditor(fileEntry)
                    && !editorLockManager.isValidDocumentKey(fileEntry, key)) {
                throw new RuntimeException(
                        MessageFormat.format(
                                "FileEntry with ID ({0}) is locked in ONLYOFFICE Docs Editor, but key ({1}) is"
                                        + "not valid",
                                String.valueOf(fileEntry.getFileEntryId()),
                                key
                        )
                );
            }

            saveFileEntry(
                    fileEntry,
                    urlManager.replaceToInnerDocumentServerUrl(fileUrl),
                    DLVersionNumberIncrease.MINOR,
                    true
            );
        }
    }

    private void handlerConnecting(final Callback callback, final FileEntry fileEntry)
            throws PortalException {
        String key = callback.getKey();

        if (editorLockManager.isLockedInEditor(fileEntry) && editorLockManager.isValidDocumentKey(fileEntry, key)) {
            Lock lock = fileEntry.getLock();

            if (lock.getExpirationTime() > 0) {
                editorLockManager.refreshTimeToExpireLock(fileEntry, EditorLockManager.TIMEOUT_INFINITY);
            }
        } else if (editorLockManager.isLockedInEditor(fileEntry)
                && !editorLockManager.isValidDocumentKey(fileEntry, key)) {
            throw new RuntimeException(
                    MessageFormat.format(
                            "FileEntry with ID ({0}) is locked in ONLYOFFICE Docs Editor, but key ({1}) is not valid",
                            String.valueOf(fileEntry.getFileEntryId()),
                            key
                    )
            );
        } else {
            if (editorLockManager.isLockedNotInEditor(fileEntry)) {
                throw new RuntimeException(
                        MessageFormat.format(
                                "FileEntry with ID ({0}) is locked not in ONLYOFFICE Docs Editor",
                                String.valueOf(fileEntry.getFileEntryId())
                        )
                );
            }

            if (fileEntry.getLock() == null) {
                EditingMeta editingMeta = new EditingMeta(key);

                editorLockManager.lockInEditor(fileEntry, editingMeta, EditorLockManager.TIMEOUT_INFINITY);
            }
        }
    }

    private void handlerDisconnecting(final Callback callback, final FileEntry fileEntry)
            throws PortalException {
        String key = callback.getKey();

        if (!editorLockManager.isLockedInEditor(fileEntry)) {
            throw new RuntimeException(
                    MessageFormat.format(
                            "FileEntry with ID ({0}) is not locked in ONLYOFFICE Docs Editor",
                            String.valueOf(fileEntry.getFileEntryId())
                    )
            );
        }

        if (editorLockManager.isLockedInEditor(fileEntry) && !editorLockManager.isValidDocumentKey(fileEntry, key)) {
            throw new RuntimeException(
                    MessageFormat.format(
                            "FileEntry with ID ({0}) is locked in ONLYOFFICE Docs Editor, but key ({1}) is not valid",
                            String.valueOf(fileEntry.getFileEntryId()),
                            key
                    )
            );
        }

        List<String> users = callback.getUsers();

        Lock lock = fileEntry.getLock();
        String lockOwner = String.valueOf(lock.getUserId());

        String currentUser = PrincipalThreadLocal.getName();

        if (users.contains(currentUser) || !lockOwner.equals(currentUser)) {
            return;
        }

        boolean lockOwnerIsChanged = false;
        for (String user : users) {
            boolean hasUpdatePermissions = PermissionUtils.checkFileEntryPermissionsForUser(
                    fileEntry,
                    ActionKeys.UPDATE,
                    Long.parseLong(user)
            );

            if (hasUpdatePermissions) {
                editorLockManager.changeLockOwner(fileEntry, Long.parseLong(user));
                lockOwnerIsChanged = true;
                break;
            }
        }

        if (!lockOwnerIsChanged) {
            throw new RuntimeException(
                    MessageFormat.format(
                            "Can not change lock owner for FileEntry with ID ({0}), "
                                    + "no user has access to the write",
                            String.valueOf(fileEntry.getFileEntryId())
                    )
            );
        }
    }

    private void saveFileEntry(final FileEntry fileEntry, final String fileUrl,
                               final DLVersionNumberIncrease numberIncrease, final boolean keepLock) {
        Lock lock = fileEntry.getLock();
        long lockOwnerUserId = lock.getUserId();
        String editingMetaAsString = lock.getOwner();

        EditingMeta editingMeta = editorLockManager.parserEditingMeta(editingMetaAsString);

        try {
            TransactionInvokerUtil.invoke(
                    TransactionConfig.Factory.create(
                            Propagation.REQUIRED, new Class<?>[] {Exception.class}),
                    () -> {
                        long currentUserId = PrincipalThreadLocal.getUserId();

                        if (currentUserId != lockOwnerUserId) {
                            editorLockManager.changeLockOwner(fileEntry, currentUserId);
                        }

                        fileEntryUtils.updateFileEntryFromUrl(
                                fileEntry,
                                fileUrl,
                                numberIncrease
                        );

                        dlAppService.checkInFileEntry(
                                fileEntry.getFileEntryId(),
                                numberIncrease,
                                "ONLYOFFICE Edit",
                                ServiceContextThreadLocal.getServiceContext()
                        );

                        if (keepLock) {
                            editorLockManager.lockInEditor(
                                    fileEntry,
                                    editingMeta,
                                    EditorLockManager.TIMEOUT_INFINITY
                            );
                        }

                        return null;
                    }
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
