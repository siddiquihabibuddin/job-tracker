package com.jobtracker.apps.domain.repo;

import com.jobtracker.apps.domain.model.ApplicationNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ApplicationNoteRepository extends JpaRepository<ApplicationNote, Long> {
    List<ApplicationNote> findAllByApplication_IdIn(Collection<UUID> applicationIds);
}