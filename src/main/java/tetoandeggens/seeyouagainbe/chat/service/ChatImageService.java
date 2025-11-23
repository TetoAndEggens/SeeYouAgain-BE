package tetoandeggens.seeyouagainbe.chat.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import tetoandeggens.seeyouagainbe.chat.dto.response.ImageUrlResponse;
import tetoandeggens.seeyouagainbe.chat.dto.response.UploadImageResponse;

@Service
@RequiredArgsConstructor
public class ChatImageService {

	private static final String S3_OBJECT_KEY_FORMAT = "chat-images/%d/%s_%s";

	@Value("${aws.s3.bucket}")
	private String bucketName;

	@Value("${aws.s3.region}")
	private String region;

	@Value("${aws.s3.access-key}")
	private String accessKey;

	@Value("${aws.s3.secret-key}")
	private String secretKey;

	public UploadImageResponse generateUploadUrl(Long chatRoomId, String fileName, String fileType) {
		AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);

		S3Presigner presigner = S3Presigner.builder()
			.region(Region.of(region))
			.credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
			.build();

		String uuid = UUID.randomUUID().toString();
		String objectKey = String.format(S3_OBJECT_KEY_FORMAT, chatRoomId, uuid, fileName);

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(bucketName)
			.key(objectKey)
			.contentType(fileType)
			.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(Duration.ofMinutes(5))
			.putObjectRequest(putObjectRequest)
			.build();

		PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
		String uploadUrl = presignedRequest.url().toString();

		presigner.close();

		return UploadImageResponse.builder()
			.uploadUrl(uploadUrl)
			.imageS3Key(objectKey)
			.build();
	}

	public ImageUrlResponse generateDownloadUrl(String imageKey) {
		AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);

		S3Presigner presigner = S3Presigner.builder()
			.region(Region.of(region))
			.credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
			.build();

		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
			.bucket(bucketName)
			.key(imageKey)
			.build();

		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
			.signatureDuration(Duration.ofHours(1))
			.getObjectRequest(getObjectRequest)
			.build();

		PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
		String downloadUrl = presignedRequest.url().toString();

		presigner.close();

		return ImageUrlResponse.builder()
			.url(downloadUrl)
			.build();
	}
}