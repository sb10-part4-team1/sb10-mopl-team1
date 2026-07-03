package com.sb10.mopl.conversation.entity;

import com.sb10.mopl.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;

@Getter
@Entity
@Table(name = "conversations")
public class Conversation extends BaseEntity {

  @LastModifiedDate
  @Column(name = "updated_at")
  private Instant updatedAt;
}
