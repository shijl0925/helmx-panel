package com.helmx.tutorial.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.helmx.tutorial.security.security.UserSessionManager;
import com.helmx.tutorial.system.dto.UserCreateRequest;
import com.helmx.tutorial.system.dto.UserUpdateRequest;
import com.helmx.tutorial.system.dto.ResetPasswordRequest;
import com.helmx.tutorial.utils.SecurityUtils;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.helmx.tutorial.dto.*;
import com.helmx.tutorial.utils.ResponseUtil;

import com.helmx.tutorial.system.dto.UserDTO;
import com.helmx.tutorial.system.entity.User;
import com.helmx.tutorial.system.mapper.UserMapper;
import com.helmx.tutorial.system.service.UserService;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/v1/auth/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserSessionManager userSessionManager;

    @Autowired
    private PasswordEncoder encoder;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Operation(summary = "Get all users")
    @GetMapping("")  // @RequestMapping(value = "", method = RequestMethod.GET)
    // @PreAuthorize("@va.check('System:User:Read')")
    public ResponseEntity<Result> GetAllUsers(
            @RequestParam(defaultValue = "1") @ApiParam(value = "当前页码") Integer page,
            @RequestParam(defaultValue = "10") @ApiParam(value = "每页数量") Integer pageSize,
            @RequestParam(required = false) @ApiParam(value = "用户名") String username,
            @RequestParam(required = false) @ApiParam(value = "昵称") String nickName,
            @RequestParam(required = false) @ApiParam(value = "邮箱") String email
    ) {

        // 参数校验
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;

        Page<User> pageInfo = new Page<>(page, pageSize);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        // 排除密码字段
        queryWrapper.select(User.class, info -> !info.getColumn().equals("password"));

        if (username != null && !username.isEmpty()) {
            queryWrapper.like("username", username);
        }
        if (nickName != null && !nickName.isEmpty()) {
            queryWrapper.like("nickname", nickName);
        }
        if (email != null && !email.isEmpty()) {
            queryWrapper.like("email", email);
        }

        Page<User> resultPage = userService.page(pageInfo, queryWrapper);

        List<User> users = resultPage.getRecords();
        Page<UserDTO> dtoPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        if (!users.isEmpty()) {
            // 批量获取用户ID
            List<Long> userIds = users.stream().map(User::getId).toList();

            // 批量获取角色信息
            Map<Long, Set<String>> roleNamesMap = userService.getUserRoleNamesBatch(userIds);
            Map<Long, Set<Long>> roleIdsMap = userService.getUserRoleIdsBatch(userIds);

            // 转换为UserDTO
            List<UserDTO> userDTOs = users.stream().map(user -> {
                UserDTO userDTO = new UserDTO(user);

                Long userId = user.getId();

                // 判断用户是否在线
                boolean online = userSessionManager.isUserOnline(user.getUsername());
                logger.info("用户{}是否在线：{}", user.getUsername(), online);
                userDTO.setOnline(online);

                Set<String> roleNames = roleNamesMap.getOrDefault(userId, new HashSet<>());
                userDTO.setRoles(roleNames);

                Set<Long> roleIds = roleIdsMap.getOrDefault(userId, new HashSet<>());
                userDTO.setRole(roleIds);


                return userDTO;
            }).toList();

            dtoPage.setRecords(userDTOs);
        } else {
            dtoPage.setRecords(new ArrayList<>());
        }

        // 使用自定义分页结果类
        PageResult<UserDTO> pageResult = new PageResult<>(dtoPage);

        return ResponseUtil.success(pageResult);
    }

    @Operation(summary = "Create a new user")
    @PostMapping("")
    // @PreAuthorize("@va.check('System:User:Create')")
    public ResponseEntity<Result> CreateUser(@RequestBody UserCreateRequest resources) {
        if (resources == null) {
            return ResponseUtil.failed(400, null, "Invalid request");
        }

        if (userService.existsByUsername(resources.getUsername())) {
            return ResponseUtil.failed(400, null, "Invalid request, username already exists");
        }
        if (userService.existsByEmail(resources.getEmail())) {
            return ResponseUtil.failed(400, null, "Invalid request, email already exists");
        }

        User user = new User();

        user.setUsername(resources.getUsername());
        user.setEmail(resources.getEmail());
        user.setNickname(resources.getNickName());
        user.setPassword(encoder.encode(resources.getPassword()));
        user.setPhone(resources.getPhone());
        user.setStatus(1);
        user.setSuperAdmin(false);

        userMapper.insert(user);

        // 保存用户角色
        Long userId = user.getId();
        userService.updateUserRoles(userId, resources.getRole());

        return ResponseUtil.success(user);
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    // @PreAuthorize("@va.check('System:User:Read')")
    public ResponseEntity<Result> GetUserById(@PathVariable Long id) {
        if (!userService.existsById(id)) {
            return ResponseUtil.failed(404, null, "User not found");
        }
        User user = userMapper.selectById(id);
        Long userId = user.getId();

        UserDTO userDTO = new UserDTO(user);
        // 获取用户角色名称
        userDTO.setRoles(userService.getUserRoleNames(userId));
        // 获取用户角色ID
        userDTO.setRole(userService.getUserRoleIds(userId));

        return ResponseUtil.success(userDTO);
    }

    @Operation(summary = "Update user by ID")
    @PutMapping("/{id}")
    // @PreAuthorize("@va.check('System:User:Update')")
    public ResponseEntity<Result> UpdateUserById(@PathVariable Long id, @RequestBody UserUpdateRequest userRequest) {
        if (!userService.existsById(id)) {
            return ResponseUtil.failed(404, null, "User not found");
        }

        User user = userService.getById(id);
        Long userId = user.getId();

        if (userRequest.getNickName() != null) {
            user.setNickname(userRequest.getNickName());
        }
        if (userRequest.getPhone() != null) {
            user.setPhone(userRequest.getPhone());
        }
        if (userRequest.getEmail() != null) {
            user.setEmail(userRequest.getEmail());
        }
        if (userRequest.getStatus() != null) {
            user.setStatus(userRequest.getStatus());
        }
        if (userRequest.getRole() != null) {
            userService.updateUserRoles(userId, userRequest.getRole());
        }


        userMapper.updateById(user);
        return ResponseUtil.success(user);
    }

    @Operation(summary = "Update User Password")
    @PutMapping("/{id}/reset_password")
    // @PreAuthorize("@va.check('System:User:Update')")
    public ResponseEntity<Result> UpdateUserPassword(@PathVariable Long id, @RequestBody ResetPasswordRequest resetPasswordRequest) {
        // 基本输入验证
        if (resetPasswordRequest.getOldPassword() == null || resetPasswordRequest.getNewPassword() == null) {
            return ResponseUtil.failed(400, null, "Password cannot be empty");
        }

        // 获取当前已认证用户
        Long userId = SecurityUtils.getCurrentUserId();

        // 只允许用户自身或超级管理员重置密码
        if (!userId.equals(id) && !userService.isSuperAdmin(userId)) {
            return ResponseUtil.failed(403, null, "No permission to reset other user's password");
        }

        User user = userMapper.selectById(id);
        if (user == null) {
            return ResponseUtil.failed(400, null, "Password update failed: User not found");
        }

        // 验证旧密码
        if (!encoder.matches(resetPasswordRequest.getOldPassword(), user.getPassword())) {
            return ResponseUtil.failed(400, null,"Password update failed: Old password is incorrect");
        }

        // 检查新旧密码是否相同
        if (encoder.matches(resetPasswordRequest.getNewPassword(), user.getPassword())) {
            return ResponseUtil.failed(400, null, "New password cannot be the same as old password");
        }

        try {
            user.setPassword(encoder.encode(resetPasswordRequest.getNewPassword()));
            userMapper.updateById(user);
            return ResponseUtil.success("Password updated successfully");
        } catch (Exception e) {
            logger.error("Password update failed: User ID {}", id, e);
            return ResponseUtil.failed(500, null, "Password update failed");
        }
    }

    @Operation(summary = "Delete user by ID")
    @DeleteMapping("/{id}")
    // @PreAuthorize("@va.check('System:User:Delete')")
    public ResponseEntity<Result> DeleteUserById(@PathVariable Long id) {
        if (!userService.existsById(id)) {
            return ResponseUtil.failed(404, null, "User not found");
        }
        userMapper.deleteById(id);
        return ResponseUtil.success(null);
    }
}
