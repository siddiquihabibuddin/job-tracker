package com.jobtracker.apps.domain.repo;

import com.jobtracker.apps.domain.model.ApplicationNote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationNoteRepository extends JpaRepository<ApplicationNote, Long> {}