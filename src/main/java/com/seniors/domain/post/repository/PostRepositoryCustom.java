package com.seniors.domain.post.repository;

import com.seniors.domain.post.dto.PostDto.GetPostRes;
import com.seniors.domain.post.dto.PostDto.ModifyPostReq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepositoryCustom {

	GetPostRes findOnePost(Long postId);
	void modifyPost(ModifyPostReq modifyPostReq, Long postId, Long userId);

	Page<GetPostRes> findAllPost(Pageable pageable);

	void removePost(Long postId, Long userId);

	void increaseLikeCount(Long postId, Integer status);
}
