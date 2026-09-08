package dev.tintwym.medicore.repo;

import dev.tintwym.medicore.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, String> {}
