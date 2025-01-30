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

package com.onlyoffice.liferay.docs.sdk.service;

import com.onlyoffice.manager.request.RequestManager;
import com.onlyoffice.service.command.CommandService;
import com.onlyoffice.service.command.DefaultCommandService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        service = CommandService.class
)
public class CommandServiceImpl extends DefaultCommandService implements CommandService {

    public CommandServiceImpl() {
        super(null);
    }

    @Reference(service = RequestManager.class, unbind = "-")
    public void setRequestManager(final RequestManager requestManager) {
        super.setRequestManager(requestManager);
    }
}
