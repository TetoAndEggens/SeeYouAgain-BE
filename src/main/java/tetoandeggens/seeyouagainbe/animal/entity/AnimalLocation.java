package tetoandeggens.seeyouagainbe.animal.entity;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tetoandeggens.seeyouagainbe.global.entity.BaseEntity;

@Entity
@Table(name = "ANIMAL_LOCATION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnimalLocation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "animal_location_id")
	private Long id;

	@Column(name = "name")
	private String name;

	@Column(name = "address")
	private String address;

	@Column(name = "coordinates", columnDefinition = "GEOMETRY", nullable = false)
	private Point coordinates;

	@Column(name = "center_no", unique = true)
	private String centerNo;

	private static final GeometryFactory geometryFactory =
		new GeometryFactory(new PrecisionModel(), 4326);

	@Builder
	public AnimalLocation(Long id, String name, String address, Double latitude, Double longitude, String centerNo) {
		this.id = id;
		this.name = name;
		this.address = address;
		this.centerNo = centerNo;
		double lon = (longitude != null) ? longitude : 0.0;
		double lat = (latitude != null) ? latitude : 0.0;
		this.coordinates = createPoint(lon, lat);
	}

	public static Point createPoint(double longitude, double latitude) {
		return geometryFactory.createPoint(new Coordinate(longitude, latitude));
	}
}
