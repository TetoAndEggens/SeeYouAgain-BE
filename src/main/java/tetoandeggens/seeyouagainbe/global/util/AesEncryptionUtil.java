package tetoandeggens.seeyouagainbe.global.util;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.ChatErrorCode;

@Component
public class AesEncryptionUtil {

	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int GCM_TAG_LENGTH = 128;
	private static final int GCM_IV_LENGTH = 12;

	private final SecretKey secretKey;

	public AesEncryptionUtil(@Value("${encryption.aes.secret-key}") String secretKeyString) {
		byte[] keyBytes = Base64.getDecoder().decode(secretKeyString);
		if (keyBytes.length != 32) {
			throw new CustomException(ChatErrorCode.INVALID_ENCRYPTION_KEY);
		}
		this.secretKey = new SecretKeySpec(keyBytes, "AES");
	}

	public String encrypt(String plainText) {
		try {
			if (plainText == null || plainText.isEmpty()) {
				return plainText;
			}

			byte[] iv = new byte[GCM_IV_LENGTH];
			SecureRandom random = new SecureRandom();
			random.nextBytes(iv);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

			byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());

			ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedBytes.length);
			byteBuffer.put(iv);
			byteBuffer.put(encryptedBytes);

			return Base64.getEncoder().encodeToString(byteBuffer.array());

		} catch (Exception e) {
			throw new CustomException(ChatErrorCode.MESSAGE_ENCRYPTION_FAILED);
		}
	}

	public String decrypt(String encryptedText) {
		try {
			if (encryptedText == null || encryptedText.isEmpty()) {
				return encryptedText;
			}

			byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);

			ByteBuffer byteBuffer = ByteBuffer.wrap(decodedBytes);
			byte[] iv = new byte[GCM_IV_LENGTH];
			byteBuffer.get(iv);

			byte[] encryptedBytes = new byte[byteBuffer.remaining()];
			byteBuffer.get(encryptedBytes);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

			byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

			return new String(decryptedBytes);

		} catch (Exception e) {
			throw new CustomException(ChatErrorCode.MESSAGE_DECRYPTION_FAILED);
		}
	}
}
