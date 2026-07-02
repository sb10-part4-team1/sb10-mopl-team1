package com.sb10.mopl.auth.repository;

import com.sb10.mopl.auth.entity.JwtSession;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JwtSessionRepository extends JpaRepository<JwtSession, UUID> {}
