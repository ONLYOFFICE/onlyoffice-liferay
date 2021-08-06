/**
 *
 * (c) Copyright Ascensio System SIA 2021
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

package onlyoffice.integration;

import java.util.Base64;
import java.util.Base64.Encoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import onlyoffice.integration.config.OnlyOfficeConfigManager;

@Component(
    service = OnlyOfficeJWT.class
)
public class OnlyOfficeJWT {

    public Boolean isEnabled() {
        String jwts = _config.getSecret();
        return jwts != null && !jwts.isEmpty();
    }

    public String createToken(JSONObject payload) {
        if (!isEnabled()) return "";

        try {
            JSONObject header = new JSONObject();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Encoder enc = Base64.getUrlEncoder();

            String encHeader = enc.encodeToString(header.toString().getBytes("UTF-8")).replace("=", "");
            String encPayload = enc.encodeToString(payload.toString().getBytes("UTF-8")).replace("=", "");
            String hash = calculateHash(encHeader, encPayload);

            return encHeader + "." + encPayload + "." + hash;
        } catch(Exception ex) {
            _log.error("Couldn't createToken: " + ex.getMessage(), ex);
            return "";
        }
    }

    public JSONObject validate(JSONObject obj, HttpServletRequest req) throws Exception {
        String token = obj.optString("token");
        Boolean inBody = true;

        if (token == null || token == "") {
            String header = req.getHeader("Authorization");
            token = (header != null && header.startsWith("Bearer ")) ? header.substring(7) : header;
            inBody = false;
        }

        if (token == null || token == "") {
            throw new SecurityException("Expected JWT");
        }

        if (!verify(token)) {
            throw new SecurityException("Wrong JWT hash");
        }

        JSONObject bodyFromToken = new JSONObject(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"));

        if (inBody) {
            return bodyFromToken;
        } else {
            return bodyFromToken.getJSONObject("payload");
        }
    }

    private Boolean verify(String token) {
        if (!isEnabled()) return false;

        String[] jwt = token.split("\\.");
        if (jwt.length != 3) return false;

        try {
            String hash = calculateHash(jwt[0], jwt[1]);
            if (!hash.equals(jwt[2])) return false;
        } catch(Exception ex) {
            _log.error("Couldn't calculate hash: " + ex.getMessage(), ex);
            return false;
        }

        return true;
    }

    private String calculateHash(String header, String payload) throws Exception {
        Mac hasher;
        hasher = getHasher();
        return Base64.getUrlEncoder().encodeToString(hasher.doFinal((header + "." + payload).getBytes("UTF-8"))).replace("=", "");
    }

    private Mac getHasher() throws Exception {
        String jwts = _config.getSecret();

        Mac sha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret = new SecretKeySpec(jwts.getBytes("UTF-8"), "HmacSHA256");
        sha256.init(secret);

        return sha256;
    }

    @Reference
    private OnlyOfficeConfigManager _config;

    private static final Log _log = LogFactoryUtil.getLog(OnlyOfficeJWT.class);
}
