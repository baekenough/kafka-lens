package com.kafkalens.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 보안 설정 프로퍼티.
 *
 * <p>application.yml의 security 섹션에서 사용자 정보를 로드합니다.</p>
 *
 * <h3>설정 예시</h3>
 * <pre>
 * security:
 *   users:
 *     - username: admin
 *       password: ${ADMIN_PASSWORD:admin}
 *       roles:
 *         - ADMIN
 * </pre>
 */
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    /**
     * 사용자 목록.
     */
    private List<User> users = new ArrayList<>();

    /**
     * 사용자 목록을 반환합니다.
     *
     * @return 사용자 목록
     */
    public List<User> getUsers() {
        return users;
    }

    /**
     * 사용자 목록을 설정합니다.
     *
     * @param users 사용자 목록
     */
    public void setUsers(List<User> users) {
        this.users = users;
    }

    /**
     * 사용자 정보.
     */
    public static class User {

        /**
         * 사용자명.
         */
        private String username;

        /**
         * 비밀번호 (평문 또는 환경 변수).
         */
        private String password;

        /**
         * 역할 목록.
         */
        private List<String> roles = new ArrayList<>();

        /**
         * 사용자명을 반환합니다.
         *
         * @return 사용자명
         */
        public String getUsername() {
            return username;
        }

        /**
         * 사용자명을 설정합니다.
         *
         * @param username 사용자명
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * 비밀번호를 반환합니다.
         *
         * @return 비밀번호
         */
        public String getPassword() {
            return password;
        }

        /**
         * 비밀번호를 설정합니다.
         *
         * @param password 비밀번호
         */
        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * 역할 목록을 반환합니다.
         *
         * @return 역할 목록
         */
        public List<String> getRoles() {
            return roles;
        }

        /**
         * 역할 목록을 설정합니다.
         *
         * @param roles 역할 목록
         */
        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }
}
