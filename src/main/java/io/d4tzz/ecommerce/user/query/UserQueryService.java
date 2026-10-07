package io.d4tzz.ecommerce.user.query;

import java.util.List;
import java.util.Optional;

public interface UserQueryService {
    Optional<UserView> findById(Long id);
    List<UserView> search(UserSearchCriteria criteria);
    long count(UserSearchCriteria criteria);
}
