package com.seniors.domain.post.service;

import com.seniors.common.exception.type.BadRequestException;
import com.seniors.domain.post.dto.PostDto;
import com.seniors.domain.post.dto.PostDto.GetPostRes;
import com.seniors.domain.post.dto.PostDto.PostCreateDto;
import com.seniors.domain.post.dto.PostDto.SavePostReq;
import com.seniors.domain.post.entity.Post;
import com.seniors.domain.post.repository.PostRepository;
import com.seniors.domain.users.entity.Users;
import com.seniors.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;


@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

	private final PostRepository postRepository;
	private final UsersRepository usersRepository;

	public void addPost(SavePostReq postReq) {
		if (postReq.getTitle() == null || postReq.getTitle().isEmpty() || postReq.getContent() == null || postReq.getContent().isEmpty()) {
			throw new BadRequestException("Title or Content is required");
		}

		usersRepository.findById(postReq.getUserId()).ifPresent(users ->
				postRepository.save(Post.of(postReq.getTitle(), postReq.getContent(), users))
		);
	}

	@Transactional
	public void removePost(Long postId) {
		postRepository.deleteById(postId);
	}

	@Transactional
	public GetPostRes findPost(Long postId, Long userId) {
		return postRepository.getOnePost(postId, userId);
	}
}
