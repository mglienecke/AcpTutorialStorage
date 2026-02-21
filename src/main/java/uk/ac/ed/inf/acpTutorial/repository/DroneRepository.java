package uk.ac.ed.inf.acpTutorial.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.ac.ed.inf.acpTutorial.entity.DroneEntity;

@Repository
public interface DroneRepository extends JpaRepository<DroneEntity, String> {

    @Query("SELECT d.name FROM DroneEntity d WHERE d.id = ?1")
    String findByName(String name);
}
