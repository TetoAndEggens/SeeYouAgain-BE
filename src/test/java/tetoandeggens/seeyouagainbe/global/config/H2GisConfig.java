package tetoandeggens.seeyouagainbe.global.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.h2gis.functions.factory.H2GISFunctions;
import org.springframework.boot.test.context.TestConfiguration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TestConfiguration
@RequiredArgsConstructor
public class H2GisConfig {

	private final DataSource dataSource;

	@PostConstruct
	public void initH2Gis() {
		try (Connection connection = dataSource.getConnection()) {
			H2GISFunctions.load(connection);
			log.info("H2GIS functions loaded successfully");
		} catch (SQLException e) {
			log.error("Failed to initialize H2GIS", e);
			throw new RuntimeException("Failed to initialize H2GIS", e);
		}
	}
}