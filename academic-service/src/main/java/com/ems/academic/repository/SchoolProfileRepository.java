package com.ems.academic.repository;

import com.ems.academic.domain.SchoolProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SchoolProfileRepository extends JpaRepository<SchoolProfile, UUID> {}