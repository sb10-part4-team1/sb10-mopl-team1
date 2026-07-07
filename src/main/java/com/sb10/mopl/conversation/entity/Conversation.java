package com.sb10.mopl.conversation.entity;

import com.sb10.mopl.common.entity.BaseUpdatableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "conversations")
public class Conversation extends BaseUpdatableEntity {

}
