package com.alejo.controllers.auth;

import com.alejo.controllers.auth.dto.*;
import com.alejo.entities.auth.Role;
import com.alejo.entities.auth.User;
import com.alejo.repository.auth.RoleRepository;
import com.alejo.security.JwtUtils;
import com.alejo.service.auth.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private IUserService userService;

    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private RoleRepository roleRepository;

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest) {
        Optional<User> userOpt = userService.findByEmail(loginRequest.getEmail());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }

        User user = userOpt.get();

        if (user.getSuspended()) {
            return ResponseEntity.status(403).body("Account is suspended. Please contact support.");
        }

        if (!user.getPassword().equals(loginRequest.getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        String token = jwtUtils.generateToken(user.getEmail());
        UserDTO userDTO = mapToDTO(user);
        AuthResponseDTO response = AuthResponseDTO.builder()
                .token(token)
                .user(userDTO)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDTO registerRequestDTO) {
        if (registerRequestDTO.getName().isEmpty() ||
                registerRequestDTO.getEmail().isEmpty() ||
                registerRequestDTO.getPassword().isEmpty()) {
            return ResponseEntity.badRequest().body("All fields are mandatory");
        }

        try {
            User newUser = userService.register(registerRequestDTO);
            String token = jwtUtils.generateToken(newUser.getEmail());
            UserDTO userDTO = mapToDTO(newUser);
            AuthResponseDTO response = AuthResponseDTO.builder()
                    .token(token)
                    .user(userDTO)
                    .build();

            return ResponseEntity.status(201).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing token");
        }

        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            return ResponseEntity.status(401).body("");
        }

        String email = jwtUtils.getUsernameFromToken(token);
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }

        User user = userOpt.get();
        if(user.getSuspended()) {
            return ResponseEntity.status(403).body("Session terminated: Account has been suspended.");
        }

        UserDTO userDTO = mapToDTO(userOpt.get());
        return ResponseEntity.ok(new AuthResponseDTO(null, userDTO));
    }

    @GetMapping("/common")
    public List<UserDTO> findAll() {
        List<User> users = userService.findAllCommon();
        return users.stream().map(this::mapToDTO).toList();
    }

    @PostMapping("/toggleBan/{id}")
    public void toggleBan(@PathVariable int id) throws InterruptedException {
         userService.toggleBan(id);
    }

    @GetMapping("/employees")
    public List<UserDTO> findAllEmployees() {
        List<User> users = userService.findAllEmployees();
        return users.stream().map(this::mapToDTO).toList();
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<?> updateEmployee(
            @PathVariable int id,
            @RequestBody UserDTO userDTO
    ) {
        try {
            Role role = roleRepository.findById(userDTO.getRole().getId())
                    .orElseThrow(() -> new RuntimeException("Role not found"));

            User updatedUser = User.builder()
                    .id(id)
                    .name(userDTO.getName())
                    .email(userDTO.getEmail())
                    .role(role)
                    .suspended(userDTO.getSuspended())
                    .build();

            User savedUser = userService.updateUser(updatedUser);
            return ResponseEntity.ok(savedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PostMapping("/employees")
    public ResponseEntity<?> addEmployee(@RequestBody AddEmployeeRequestDTO dto) {
        try {
            User newUser = userService.addEmployee(dto);
            return ResponseEntity.status(201).body(newUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }


    private UserDTO mapToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .suspended(user.getSuspended())
                .role(user.getRole())
                .build();
    }
}
