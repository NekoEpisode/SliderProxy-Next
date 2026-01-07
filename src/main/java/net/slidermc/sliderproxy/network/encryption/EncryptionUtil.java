package net.slidermc.sliderproxy.network.encryption;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;

/**
 * Minecraft 协议加密工具类
 */
public class EncryptionUtil {

    private static final String RSA_ALGORITHM = "RSA";
    private static final String AES_ALGORITHM = "AES/CFB8/NoPadding";
    private static final int RSA_KEY_SIZE = 1024;

    /**
     * 生成 RSA 密钥对
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            generator.initialize(RSA_KEY_SIZE);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

    /**
     * 生成随机验证令牌
     */
    public static byte[] generateVerifyToken() {
        byte[] token = new byte[4];
        new SecureRandom().nextBytes(token);
        return token;
    }

    /**
     * 使用 RSA 私钥解密数据
     */
    public static byte[] decryptRsa(PrivateKey privateKey, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(data);
    }

    /**
     * 使用 RSA 公钥加密数据
     */
    public static byte[] encryptRsa(PublicKey publicKey, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    /**
     * 从 DER 编码的字节数组创建公钥
     */
    public static PublicKey decodePublicKey(byte[] encodedKey) throws Exception {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(encodedKey);
        KeyFactory factory = KeyFactory.getInstance(RSA_ALGORITHM);
        return factory.generatePublic(spec);
    }

    /**
     * 创建 AES 加密 Cipher
     */
    public static Cipher createEncryptCipher(SecretKey sharedSecret) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, sharedSecret, new IvParameterSpec(sharedSecret.getEncoded()));
        return cipher;
    }

    /**
     * 创建 AES 解密 Cipher
     */
    public static Cipher createDecryptCipher(SecretKey sharedSecret) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, sharedSecret, new IvParameterSpec(sharedSecret.getEncoded()));
        return cipher;
    }

    /**
     * 从字节数组创建 AES SecretKey
     */
    public static SecretKey createSecretKey(byte[] sharedSecret) {
        return new SecretKeySpec(sharedSecret, "AES");
    }

    /**
     * 计算 Minecraft 风格的服务器哈希
     * Minecraft 使用非标准的十六进制摘要方法：
     * - 将 SHA-1 输出视为一个大整数（二进制补码）
     * - 如果是负数，输出带负号的十六进制
     */
    public static String computeServerIdHash(String serverId, byte[] sharedSecret, byte[] publicKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(serverId.getBytes(StandardCharsets.ISO_8859_1));
            digest.update(sharedSecret);
            digest.update(publicKey);
            return new BigInteger(digest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
    }
}
