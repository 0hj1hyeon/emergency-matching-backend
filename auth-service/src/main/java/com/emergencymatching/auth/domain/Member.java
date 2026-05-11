package com.emergencymatching.auth.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;    

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    public Member(String username, String password, MemberRole role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }
}
