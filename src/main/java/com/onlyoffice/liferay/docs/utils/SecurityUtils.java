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

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.SecureRandom;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import org.osgi.service.component.annotations.Component;

import javax.servlet.http.HttpServletRequest;


@Component(
        service = SecurityUtils.class
)
public final class SecurityUtils {
    public static final String NAME_ALLOWED_CHARACTERS
            = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private SecurityUtils() {
    }

    public static String generateSecret(final int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder key = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(NAME_ALLOWED_CHARACTERS.length());
            key.append(NAME_ALLOWED_CHARACTERS.charAt(index));
        }

        return key.toString();
    }

    public static User setUserAuthentication(final HttpServletRequest httpServletRequest) throws PortalException {
        User user = PortalUtil.getUser(httpServletRequest);

        return setUserAuthentication(user);
    }

    public static User setUserAuthentication(final User user) throws PortalException {
        if (user == null) {
            throw new PortalException();
        }

        PrincipalThreadLocal.setName(user.getUserId());
        PermissionThreadLocal.setPermissionChecker(PermissionCheckerFactoryUtil.create(user));

        return user;
    }

    public static <R> R runAs(final RunAsWork<R> runAsWork, final long userId) throws Exception {
        User user = UserLocalServiceUtil.getUser(userId);

        String name = PrincipalThreadLocal.getName();
        PermissionChecker permissionChecker = PermissionThreadLocal.getPermissionChecker();

        try {
            PrincipalThreadLocal.setName(userId);
            PermissionThreadLocal.setPermissionChecker(PermissionCheckerFactoryUtil.create(user));

            return runAsWork.doWork();
        } finally {
            PrincipalThreadLocal.setName(name);
            PermissionThreadLocal.setPermissionChecker(permissionChecker);
        }
    }

    public interface RunAsWork<Result> {
        Result doWork() throws Exception;
    }
}
