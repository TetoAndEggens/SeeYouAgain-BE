package tetoandeggens.seeyouagainbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class SeeYouAgainBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(SeeYouAgainBeApplication.class, args);
	}

}
