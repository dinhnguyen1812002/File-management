package com.app.file_transfer.repository;


import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "persistent_logins")
public class PersistentLogins {

    @Id
    @Column(name = "series", length = 64)
    private String series;

    @Column(name = "username", length = 64, nullable = false)
    private String username;

    @Column(name = "token", length = 64, nullable = false)
    private String token;

    @Column(name = "last_used", nullable = false)
    private LocalDateTime lastUsed;

    // Constructors
    public PersistentLogins() {}

    public PersistentLogins(String series, String username, String token, LocalDateTime lastUsed) {
        this.series = series;
        this.username = username;
        this.token = token;
        this.lastUsed = lastUsed;
    }

    // Getters and Setters
    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
    }
}