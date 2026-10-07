package io.d4tzz.ecommerce.user.api;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.query.PageResponse;
import io.d4tzz.ecommerce.user.query.UserQueryService;
import io.d4tzz.ecommerce.user.query.UserSearchCriteria;
import io.d4tzz.ecommerce.user.query.UserView;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserQueryService userQueryService;

    public AdminUserController(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserView>> listUsers(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Boolean emailVerified,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        var criteria = new UserSearchCriteria(keyword, role, status, emailVerified, page, size);
        List<UserView> users = userQueryService.search(criteria);
        long total = userQueryService.count(criteria);
        return ResponseEntity.ok(PageResponse.of(users, page, size, total));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserView> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(
            userQueryService.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id))
        );
    }
}
