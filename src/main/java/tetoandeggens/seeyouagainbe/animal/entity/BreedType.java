package tetoandeggens.seeyouagainbe.animal.entity;

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
@Table(name = "BREED_TYPE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BreedType extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "breed_type_id")
	private Long id;

	@Column(name = "name")
	private String name;

	@Column(name = "type")
	private String type;

    @Column(name = "code", unique = true)
    private String code;

	@Builder
	public BreedType(String name, String type, String code) {
		this.name = name;
		this.type = type;
		this.code = code;
	}
}