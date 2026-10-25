package com.lab.userservice;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimecodedNoteRepository extends JpaRepository<TimecodedNote, Long> {
    List<TimecodedNote> findByClipIdOrderByTimecodeAsc(Long clipId);
}
