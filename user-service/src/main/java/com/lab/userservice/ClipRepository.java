package com.lab.userservice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClipRepository extends JpaRepository<Clip, Long> {
    long countByStatus(String status);
}
