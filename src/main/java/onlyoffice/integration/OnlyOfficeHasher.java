package onlyoffice.integration;

import java.security.MessageDigest;
import java.util.Base64;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import onlyoffice.integration.config.OnlyOfficeConfigManager;

@Component(
    service = OnlyOfficeHasher.class
)
public class OnlyOfficeHasher {
    public String getHash(Long id) {
        try
        {
            String str = Long.toString(id);

            String payload = getHashString(str + getSecret()) + "?" + str;
            return Base64.getUrlEncoder().encodeToString(payload.getBytes("UTF-8"));
        }
        catch (Exception ex)
        {
            _log.error(ex.getMessage(), ex);
        }
        return "";
    }

    public Long validate(String base64)
    {
        try
        {
            String payload = new String(Base64.getUrlDecoder().decode(base64), "UTF-8");

            String[] payloadParts = payload.split("\\?");

            String hash = getHashString(payloadParts[1] + getSecret());
            if (hash.equals(payloadParts[0]))
            {
                return Long.parseLong(payloadParts[1]);
            }
        } catch (Exception ex)
        {
            _log.error(ex.getMessage(), ex);
        }
        return (long) 0;
    }

    private String getSecret() {
        return _config.getSecret();
    }

    private String getHashString(String str)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(str.getBytes());
            String b64 = Base64.getEncoder().encodeToString(digest);

            return b64;
        } catch (Exception ex)
        {
            _log.error(ex.getMessage(), ex);
        }
        return "";
    }

    @Reference
    private OnlyOfficeConfigManager _config;

    private static final Log _log = LogFactoryUtil.getLog(OnlyOfficeHasher.class);
}
