package com.myschool.userservice.controller;

import com.myschool.userservice.dto.UserResponse;
import com.myschool.userservice.entity.User;
import com.myschool.userservice.repository.UserRepository;
import com.myschool.userservice.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService  userService;
    public UserController( UserService userService ) {
        this.userService = userService;
    }
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.create(user);
    }
    @GetMapping
    public List<UserResponse> getAll() {
        return userService.getAll()
                .stream()
                .map(u -> new UserResponse(
                        u.getId(),
                        u.getFullName(),
                        u.getEmail()
                ))
                .toList();
    }
    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
       return userService.getById(id);
    }

    // UPDATE
    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody User payload) {
        return userService.update(id, payload);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
