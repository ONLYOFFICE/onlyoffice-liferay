package onlyoffice.integration.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;


@ObjectClassDefinition(
    id = "onlyoffice.integration.config.OnlyOfficeConfiguration",
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

}