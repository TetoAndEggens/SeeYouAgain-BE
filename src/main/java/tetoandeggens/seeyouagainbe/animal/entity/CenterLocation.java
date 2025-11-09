package tetoandeggens.seeyouagainbe.animal.entity;

import org.locationtech.jts.geom.Point;

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
@Table(name = "CENTER_LOCATION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CenterLocation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "center_location_id")
	private Long id;

	@Column(name = "name")
	private String name;

	@Column(name = "address")
	private String address;

	@Column(name = "coordinates")
	private Point coordinates;

	@Column(name = "center_no", unique = true)
	private String centerNo;

	@Builder
	public CenterLocation(Long id, String name, String address, Point coordinates, String centerNo) {
		this.id = id;
		this.name = name;
		this.address = address;
		this.coordinates = coordinates;
		this.centerNo = centerNo;
	}
}