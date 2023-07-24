package com.seniors.domain.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLikeId implements Serializable {

    @Column(name = "postId")
    private Long postId;

    @Column(name = "userId")
    private Long userId;
    // equals, hashCode 메서드 등을 재정의할 수 있음
}
