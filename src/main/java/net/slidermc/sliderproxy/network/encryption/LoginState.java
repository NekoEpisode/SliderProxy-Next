package net.slidermc.sliderproxy.network.encryption;

import io.netty.util.AttributeKey;
import net.slidermc.sliderproxy.api.player.data.GameProfile;
import net.slidermc.sliderproxy.network.packet.clientbound.login.ClientboundLoginSuccessPacket;

import java.util.List;

/**
 * 登录状态 - 存储登录过程中的临时数据
 */
public class LoginState {
    public static final AttributeKey<LoginState> KEY = AttributeKey.valueOf("login_state");
    private final String username;
    private final java.util.UUID clientUuid;
    private final byte[] verifyToken;
    private GameProfile authenticatedProfile;
    private List<ClientboundLoginSuccessPacket.Property> properties;

    public LoginState(String username, java.util.UUID clientUuid, byte[] verifyToken) {
        this.username = username;
        this.clientUuid = clientUuid;
        this.verifyToken = verifyToken;
    }

    public String getUsername() {
        return username;
    }

    public java.util.UUID getClientUuid() {
        return clientUuid;
    }

    public byte[] getVerifyToken() {
        return verifyToken;
    }

    public GameProfile getAuthenticatedProfile() {
        return authenticatedProfile;
    }

    public void setAuthenticatedProfile(GameProfile authenticatedProfile) {
        this.authenticatedProfile = authenticatedProfile;
    }

    public List<ClientboundLoginSuccessPacket.Property> getProperties() {
        return properties;
    }

    public void setProperties(List<ClientboundLoginSuccessPacket.Property> properties) {
        this.properties = properties;
    }
}
