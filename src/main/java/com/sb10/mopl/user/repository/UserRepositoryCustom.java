package com.sb10.mopl.user.repository;

import com.sb10.mopl.user.dto.UserSearchRequest;
import com.sb10.mopl.user.entity.User;
import java.util.List;

public interface UserRepositoryCustom {

  List<User> findAllByCondition(UserSearchRequest request);

  long countByCondition(UserSearchRequest request);
}
