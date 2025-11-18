package tetoandeggens.seeyouagainbe.image.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class ImageService {

	private static final String S3_OBJECT_KEY_FORMAT = "animal-profiles/user/%d_%s";

	@Value("${aws.s3.bucket}")
	private String bucketName;

	@Value("${aws.s3.region}")
	private String region;

	@Value("${aws.s3.access-key}")
	private String accessKey;

	@Value("${aws.s3.secret-key}")
	private String secretKey;

	public List<String> generatePresignedUrls(Long animalId, int count) {
		List<String> presignedUrls = new ArrayList<>();

		AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);

		S3Presigner presigner = S3Presigner.builder()
			.region(Region.of(region))
			.credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
			.build();

		for (int i = 0; i < count; i++) {
			String uuid = UUID.randomUUID().toString();
			String objectKey = String.format(S3_OBJECT_KEY_FORMAT, animalId, uuid);

			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(bucketName)
				.key(objectKey)
				.build();

			PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
				.signatureDuration(Duration.ofMinutes(10))
				.putObjectRequest(putObjectRequest)
				.build();

			PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
			presignedUrls.add(presignedRequest.url().toString());
		}

		presigner.close();

		return presignedUrls;
	}
}
