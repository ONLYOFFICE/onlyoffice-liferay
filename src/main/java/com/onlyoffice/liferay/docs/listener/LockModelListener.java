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

package com.onlyoffice.liferay.docs.listener;

import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.ModelListener;
import com.liferay.portal.lock.model.Lock;
import com.onlyoffice.liferay.docs.model.EditingMeta;
import com.onlyoffice.liferay.docs.sdk.service.CallbackServiceImpl;
import com.onlyoffice.liferay.docs.ui.EditMenuContext;
import com.onlyoffice.liferay.docs.utils.FileEntryUtils;
import com.onlyoffice.model.commandservice.CommandRequest;
import com.onlyoffice.model.commandservice.CommandResponse;
import com.onlyoffice.model.commandservice.commandrequest.Command;
import com.onlyoffice.service.command.CommandService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.Arrays;
import java.util.List;

@Component(service = ModelListener.class)
public class LockModelListener extends BaseModelListener<Lock> {
    private static final Log log = LogFactoryUtil.getLog(LockModelListener.class);

    @Reference
    private CommandService commandService;
    @Reference
    private FileEntryUtils fileEntryUtils;

    @Override
    public void onAfterRemove(final Lock lock) throws ModelListenerException {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        boolean unlockedFormEditor = Arrays.stream(stackTrace).sequential()
                .anyMatch(stackTraceElement ->
                    stackTraceElement.getClassName().equals(CallbackServiceImpl.class.getName())
                );

        if (unlockedFormEditor) {
            return;
        }

        EditingMeta editingMeta = fileEntryUtils.getEditingMeta(lock.getOwner());

        if (editingMeta == null) {
            return;
        }

        List<String> users;
        CommandResponse infoResponse;
        try {
            CommandRequest infoRequest = CommandRequest.builder()
                    .c(Command.INFO)
                    .key(editingMeta.getEditingKey())
                    .build();

            infoResponse = commandService.processCommand(infoRequest, null);
            users = infoResponse.getUsers();
        } catch (Exception e) {
            log.error(e, e);

            return;
        }

        try {
            CommandRequest dropRequest = CommandRequest.builder()
                    .c(Command.DROP)
                    .key(editingMeta.getEditingKey())
                    .users(users)
                    .build();

            commandService.processCommand(dropRequest, null);
        } catch (Exception e) {
            log.error(e, e);
        }
    }
}
