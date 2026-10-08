package io.d4tzz.ecommerce.user.inbound.rest;

import io.d4tzz.ecommerce.common.exception.NotFoundException;
import io.d4tzz.ecommerce.query.PageResponse;
import io.d4tzz.ecommerce.user.query.UserQueryService;
import io.d4tzz.ecommerce.user.query.UserSearchCriteria;
import io.d4tzz.ecommerce.user.query.UserView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@Tag(name = "Admin Users", description = "User management for admins")
public class AdminUserController {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserQueryService userQueryService;

    public AdminUserController(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    @Operation(
        summary = "Search users",
        description = "Paged user list. All filters are optional. Page size is capped at 100."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "A page of users")
    })
    @GetMapping
    public ResponseEntity<PageResponse<UserView>> listUsers(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Boolean emailVerified,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        var criteria = new UserSearchCriteria(keyword, role, status, emailVerified, page, size);
        List<UserView> users = userQueryService.search(criteria);
        long total = userQueryService.count(criteria);
        return ResponseEntity.ok(PageResponse.of(users, page, size, total));
    }

    @Operation(
        summary = "Get a user",
        description = "Returns one user by id."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The user"),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserView> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(
            userQueryService.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id))
        );
    }
}
