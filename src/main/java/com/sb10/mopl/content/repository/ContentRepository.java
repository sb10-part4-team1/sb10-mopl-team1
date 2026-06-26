package com.sb10.mopl.content.repository;

import com.sb10.mopl.content.entity.Content;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID>, ContentRepositoryCustom {}
