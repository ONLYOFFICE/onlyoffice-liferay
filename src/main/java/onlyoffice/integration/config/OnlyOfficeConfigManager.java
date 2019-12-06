package onlyoffice.integration.config;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = OnlyOfficeConfiguration.class)
@Component(
    configurationPid = {
        "onlyoffice.integration.config.OnlyOfficeConfiguration"
    },
    immediate = true,
    service = OnlyOfficeConfigManager.class
)
public class OnlyOfficeConfigManager {

    public String getDocUrl() {
        return configuration.docServUrl();
    }

    public String getDocInnerUrlOrDefault(String def) {
        String url = configuration.docServUrl();
        return url == null || url.isEmpty() ? def : url;
    }

    public String getLiferayUrlOrDefault(String def) {
        String url = configuration.liferayUrl();
        return url == null || url.isEmpty() ? def : url;
    }

    public String getSecret() {
        return configuration.secret();
    }

    @Activate
    @Modified
    protected void readConfig(OnlyOfficeConfiguration config) {
        this.configuration = config;
    }

    private OnlyOfficeConfiguration configuration;
}
