package uk.gov.dvla.siftconfig.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import uk.gov.dvla.siftconfig.model.WriteConfig;

public interface WriteConfigRepository extends JpaRepository<WriteConfig, String> {
}
