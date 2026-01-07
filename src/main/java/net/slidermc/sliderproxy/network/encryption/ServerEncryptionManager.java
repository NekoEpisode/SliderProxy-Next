package net.slidermc.sliderproxy.network.encryption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * 服务器加密管理器 - 管理 RSA 密钥对
 */
public class ServerEncryptionManager {
    private static final Logger log = LoggerFactory.getLogger(ServerEncryptionManager.class);
    private static ServerEncryptionManager instance;

    private final KeyPair keyPair;
    private final byte[] publicKeyEncoded;

    private ServerEncryptionManager() {
        log.info("Generating RSA key pair for encryption...");
        this.keyPair = EncryptionUtil.generateKeyPair();
        this.publicKeyEncoded = keyPair.getPublic().getEncoded();
        log.info("RSA key pair generated successfully");
    }

    public static synchronized ServerEncryptionManager getInstance() {
        if (instance == null) {
            instance = new ServerEncryptionManager();
        }
        return instance;
    }

    /**
     * 获取公钥
     */
    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    /**
     * 获取私钥
     */
    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    /**
     * 获取 DER 编码的公钥字节数组
     */
    public byte[] getPublicKeyEncoded() {
        return publicKeyEncoded;
    }
}
