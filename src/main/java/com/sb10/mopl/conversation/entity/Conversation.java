package com.sb10.mopl.conversation.entity;

import com.sb10.mopl.common.entity.BaseUpdatableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
@Entity
@Table(name = "conversations")
public class Conversation extends BaseUpdatableEntity {

  // 연관관계를 파악 및 생성 시 cascade로 함께 저장되도록 하기 위한 코드
  @OneToMany(mappedBy = "conversation", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
  private List<ConversationParticipant> participants = new ArrayList<>();
}
