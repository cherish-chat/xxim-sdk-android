package chat.cherish.xxim.sdk.common;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class CXNParams {
    public String platform;
    public String deviceId;
    public String deviceModel;
    public String osVersion;
    public String appVersion;
    public String language;
    public String networkUsed;
    public byte[] ext;

    public CXNParams(String platform, String deviceId, String deviceModel, String osVersion, String appVersion,
                     String language, String networkUsed
    ) {
        this.platform = platform;
        this.deviceId = deviceId;
        this.deviceModel = deviceModel;
        this.osVersion = osVersion;
        this.appVersion = appVersion;
        this.language = language;
        this.networkUsed = networkUsed;
        this.ext = "".getBytes(StandardCharsets.UTF_8);
    }
}
