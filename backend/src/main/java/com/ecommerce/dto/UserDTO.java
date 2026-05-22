package com.ecommerce.dto;

import java.time.LocalDateTime;

public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private Integer age;
    private Integer gender;
    private String city;
    private String avatar;
    private String role;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String username;
        private String email;
        private String phone;
        private Integer age;
        private Integer gender;
        private String city;
        private String avatar;
        private String role;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder age(Integer age) { this.age = age; return this; }
        public Builder gender(Integer gender) { this.gender = gender; return this; }
        public Builder city(String city) { this.city = city; return this; }
        public Builder avatar(String avatar) { this.avatar = avatar; return this; }
        public Builder role(String role) { this.role = role; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public UserDTO build() {
            UserDTO r = new UserDTO();
            r.id = this.id; r.username = this.username; r.email = this.email;
            r.phone = this.phone; r.age = this.age; r.gender = this.gender;
            r.city = this.city; r.avatar = this.avatar; r.role = this.role;
            r.createdAt = this.createdAt;
            return r;
        }
    }
}