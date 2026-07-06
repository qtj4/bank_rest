package com.example.bankcards.service;

import com.example.bankcards.dto.request.UserCreateRequest;
import com.example.bankcards.dto.request.UserUpdateRequest;
import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.exception.ConflictException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.repository.spec.UserSpecifications;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;
    private final MessageService messageService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper,
            CurrentUserService currentUserService,
            MessageService messageService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.currentUserService = currentUserService;
        this.messageService = messageService;
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException(messageService.get("business.username.taken"));
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setRole(request.getRole());
        user.setEnabled(request.getEnabled() == null || request.getEnabled());
        try {
            return userMapper.toResponse(userRepository.save(user));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(messageService.get("business.username.taken"));
        }
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return userMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(String search, Role role, Boolean enabled, Pageable pageable) {
        Specification<User> specification = Specification.allOf(
                UserSpecifications.notDeleted(),
                UserSpecifications.search(search),
                UserSpecifications.role(role),
                UserSpecifications.enabled(enabled)
        );
        return userRepository.findAll(specification, pageable).map(userMapper::toResponse);
    }

    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest request) {
        User user = getEntity(id);
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        return userMapper.toResponse(user);
    }

    @Transactional
    public void disable(UUID id) {
        User user = getEntity(id);
        user.setEnabled(false);
        user.markDeleted(currentUserService.getCurrentUser().getId());
    }

    public User getEntity(UUID id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException(messageService.get("business.user.not-found")));
    }
}
