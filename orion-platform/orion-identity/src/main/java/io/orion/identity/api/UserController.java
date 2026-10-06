package io.orion.identity.api;

import io.orion.identity.api.dto.request.CreateUserRequest;
import io.orion.identity.api.dto.response.UserResponse;
import io.orion.identity.application.UserService;
import io.orion.shared.paging.PageResult;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService users;
    public UserController(UserService users) { this.users = users; }

    @GetMapping("/me")
    public UserResponse me() { return UserResponse.from(users.getCurrentUser()); }

    @PostMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest r) {
        return UserResponse.from(users.createUser(
                new UserService.CreateUserCommand(r.email(), r.name(), r.password(), r.roles())));
    }

    @GetMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public PageResult<UserResponse> list(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return users.listUsers(page, size).map(UserResponse::from);
    }
}
