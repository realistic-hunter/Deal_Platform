package com.example.dealplatform.service;

import com.example.dealplatform.model.User;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class UserService {
    private final JdbcTemplate jdbcTemplate;

    public UserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<User> rowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setRoleId(rs.getLong("role_id"));
        user.setRoleCode(rs.getString("role_code"));
        user.setRoleName(rs.getString("role_name"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setRealName(rs.getString("real_name"));
        user.setNickname(rs.getString("nickname"));
        user.setStudentNo(rs.getString("student_no"));
        user.setPhone(rs.getString("phone"));
        user.setEmail(rs.getString("email"));
        user.setStatus(rs.getInt("status"));
        user.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        return user;
    };

    public Optional<User> login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return Optional.empty();
        }
        try {
            User user = jdbcTemplate.queryForObject(baseSql() + " WHERE u.username = ? AND u.password = ? AND u.is_deleted = 0",
                    rowMapper, username.trim(), password.trim());
            if (user == null || user.getStatus() == 0) {
                return Optional.empty();
            }
            jdbcTemplate.update("UPDATE user SET last_login_time = NOW() WHERE id = ?", user.getId());
            return Optional.of(user);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public void register(String username, String password, String confirmPassword, String phone, String email) {
        require(username, "用户名不能为空");
        require(password, "密码不能为空");
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        if (StringUtils.hasText(phone) && !phone.matches("^1[3-9]\\d{9}$")) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        if (StringUtils.hasText(email) && !email.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE username = ? OR (phone IS NOT NULL AND phone = ?) OR (email IS NOT NULL AND email = ?)",
                Integer.class, username.trim(), phone, email);
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("用户名、手机号或邮箱已存在");
        }
        jdbcTemplate.update("""
                INSERT INTO user(role_id, username, password, nickname, phone, email, status)
                VALUES(2, ?, ?, ?, ?, ?, 1)
                """, username.trim(), password.trim(), username.trim(), emptyToNull(phone), emptyToNull(email));
    }

    public User findById(Long id) {
        return jdbcTemplate.queryForObject(baseSql() + " WHERE u.id = ?", rowMapper, id);
    }

    public void updateProfile(Long id, String nickname, String realName, String phone, String email) {
        require(nickname, "昵称不能为空");
        if (StringUtils.hasText(phone) && !phone.matches("^1[3-9]\\d{9}$")) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        if (StringUtils.hasText(email) && !email.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE id <> ? AND ((phone IS NOT NULL AND phone = ?) OR (email IS NOT NULL AND email = ?))",
                Integer.class, id, phone, email);
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("手机号或邮箱已被占用");
        }
        jdbcTemplate.update("UPDATE user SET nickname = ?, real_name = ?, phone = ?, email = ? WHERE id = ?",
                nickname.trim(), emptyToNull(realName), emptyToNull(phone), emptyToNull(email), id);
    }

    public void changePassword(Long id, String oldPassword, String newPassword, String confirmPassword) {
        require(oldPassword, "旧密码不能为空");
        require(newPassword, "新密码不能为空");
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("两次输入的新密码不一致");
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user WHERE id = ? AND password = ?",
                Integer.class, id, oldPassword);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("旧密码不正确");
        }
        jdbcTemplate.update("UPDATE user SET password = ? WHERE id = ?", newPassword, id);
    }

    private String baseSql() {
        return """
                SELECT u.*, r.role_code, r.role_name
                FROM user u
                JOIN role r ON u.role_id = r.id
                """;
    }

    private void require(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
