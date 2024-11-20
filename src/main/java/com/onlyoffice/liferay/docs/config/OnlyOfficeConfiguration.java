/**
 *
 * (c) Copyright Ascensio System SIA 2023
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

package com.onlyoffice.liferay.docs.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;


@ObjectClassDefinition(
    id = "com.onlyoffice.liferay.docs.config.OnlyOfficeConfiguration",
    localization = "content/Language",
    name = "onlyoffice-config-name", description = "onlyoffice-config-desc"
)
@ExtendedObjectClassDefinition(
    category = "connectors",
    scope = ExtendedObjectClassDefinition.Scope.SYSTEM
)
public @interface OnlyOfficeConfiguration {

    @AttributeDefinition(
        required = true,
        name = "onlyoffice-config-docserv-url-name", description = "onlyoffice-config-docserv-url-desc"
    )
    public String docServUrl() default "http://127.0.0.1/";

    @AttributeDefinition(
        required = false,
        name = "onlyoffice-config-docserv-inner-url-name", description = "onlyoffice-config-docserv-inner-url-desc"
    )
    public String docServInnerUrl() default "";

    @AttributeDefinition(
        required = false,
        name = "onlyoffice-config-liferay-url-name", description = "onlyoffice-config-liferay-url-desc"
    )
    public String liferayUrl() default "";

    @AttributeDefinition(
        required = false,
        name = "onlyoffice-config-secret-name", description = "onlyoffice-config-secret-desc"
    )
    public String secret() default "";

    @AttributeDefinition(
        required = false,
        name = "onlyoffice-config-jwt-header", description = "onlyoffice-config-jwt-header-desc"
    )
    public String jwtHeader() default "";

    @AttributeDefinition(
        required = false,
        name = "onlyoffice-config-force-save-name", description = "onlyoffice-config-force-save-desc"
    )
    public boolean forceSave() default false;

    @AttributeDefinition(
        required = false,
        name = "onlyoffice-config-webpreview"
    )
    public boolean webPreview() default false;
}
