package tetoandeggens.seeyouagainbe.animal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tetoandeggens.seeyouagainbe.animal.entity.BookMark;
import tetoandeggens.seeyouagainbe.animal.repository.custom.BookMarkRepositoryCustom;

public interface BookMarkRepository extends JpaRepository<BookMark, Long>, BookMarkRepositoryCustom {
}
