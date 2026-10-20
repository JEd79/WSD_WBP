package uk.gov.dvla.siftconfig.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import uk.gov.dvla.siftconfig.model.ReadConfig;

public interface ReadConfigRepository extends JpaRepository<ReadConfig, String> {

    Optional<ReadConfig> findByPattern(String pattern);

    boolean existsByPattern(String pattern);
}
