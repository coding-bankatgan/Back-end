package com.onedrinktoday.backend.domain.post.service;

import com.onedrinktoday.backend.domain.autoComplete.AutoCompleteService;
import com.onedrinktoday.backend.domain.drink.dto.DrinkResponse;
import com.onedrinktoday.backend.domain.drink.entity.Drink;
import com.onedrinktoday.backend.domain.drink.repository.DrinkRepository;
import com.onedrinktoday.backend.domain.postLike.entity.PostLike;
import com.onedrinktoday.backend.domain.postLike.repository.PostLikeRepository;
import com.onedrinktoday.backend.domain.search.SearchService;
import com.onedrinktoday.backend.domain.member.entity.Member;
import com.onedrinktoday.backend.domain.member.repository.MemberRepository;
import com.onedrinktoday.backend.domain.member.service.MemberService;
import com.onedrinktoday.backend.domain.notification.service.NotificationService;
import com.onedrinktoday.backend.domain.post.dto.PostRequest;
import com.onedrinktoday.backend.domain.post.dto.PostResponse;
import com.onedrinktoday.backend.domain.post.entity.Post;
import com.onedrinktoday.backend.domain.post.repository.PostRepository;
import com.onedrinktoday.backend.domain.postTag.entity.PostTag;
import com.onedrinktoday.backend.domain.postTag.repository.PostTagRepository;
import com.onedrinktoday.backend.domain.tag.entity.Tag;
import com.onedrinktoday.backend.domain.tag.repository.TagRepository;
import com.onedrinktoday.backend.global.cache.CacheService;
import com.onedrinktoday.backend.global.exception.CustomException;
import com.onedrinktoday.backend.global.exception.ErrorCode;
import com.onedrinktoday.backend.global.type.Role;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {

  private final PostRepository postRepository;
  private final PostTagRepository postTagRepository;
  private final PostLikeRepository postLikeRepository;
  private final TagRepository tagRepository;
  private final MemberRepository memberRepository;
  private final DrinkRepository drinkRepository;
  private final MemberService memberService;
  private final CacheManager cacheManager;
  private final CacheService cacheService;
  private final NotificationService notificationService;
  private final SearchService searchService;
  private final AutoCompleteService autoCompleteService;

  // 게시글 생성 및 저장
  @CacheEvict(key = "#postRequest.drinkId", value = "avg-rating")
  public PostResponse createPost(PostRequest postRequest) {

    Member member = memberService.getMember();

    Drink drink = drinkRepository.findById(postRequest.getDrinkId())
        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 특산주입니다."));

    // 이미지 우선순위 설정 - 1순위 : 게시글 업로드 이미지, 2순위 : 특산주 등록 이미지
    String imageUrl = postRequest.getImageUrl() != null ? postRequest.getImageUrl() : drink.getImageUrl();

    // 게시글 엔티티 생성
    Post post = Post.builder()
        .member(member)
        .drink(drink)
        .type(postRequest.getType())
        .content(postRequest.getContent())
        .rating(postRequest.getRating())
        .viewCount(0)  // 초기 조회수
        .likeCount(0)  // 초기 좋아요수
        .imageUrl(imageUrl)
        .build();

    // 게시글 저장
    post = postRepository.save(post);

    // 태그 저장 및 PostTag 연결
    List<Tag> tags = saveTags(postRequest.getTag(), post);

    notificationService.tagFollowPostNotification(post.getId(), tags);
    searchService.save(post, tags);

    return PostResponse.of(post, tags, false);
  }

  // 태그 저장
  private List<Tag> saveTags(List<String> tagNames, Post post) {
    return tagNames.stream().map(tagName -> {
      Tag tag = tagRepository.findByTagName(tagName)
          .orElseGet(() -> {
            Tag newTag = new Tag();
            newTag.setTagName(tagName);
            autoCompleteService.saveAutoCompleteTag(tagName);
            return tagRepository.save(newTag);
          });

      postTagRepository.findByPostAndTag(post, tag)
          .orElseGet(() -> {
            PostTag newPostTag = new PostTag();
            newPostTag.setPost(post);
            newPostTag.setTag(tag);
            return postTagRepository.save(newPostTag);
          });

      return tag;
    }).collect(Collectors.toList());
  }

  // 전체 게시글 조회
  public Page<PostResponse> getAllPosts(Pageable pageable, String sortBy, Long memberId) {
    Page<Post> posts;

    if ("viewCount".equals(sortBy)) {
      posts = postRepository.findAllByOrderByViewCountDesc(pageable); // 조회수 정렬
    } else {
      posts = postRepository.findAllByOrderByCreatedAtDesc(pageable); // 최신순 정렬
    }

    // posts가 null이 아닌지 확인
    if (posts == null || posts.isEmpty()) {
      return Page.empty(pageable);  // 빈 페이지 반환
    }

    Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자입니다."));

    return posts.map(post -> {
      List<Tag> tags = postTagRepository.findTagsByPostId(post.getId());
      boolean alreadyLiked = postLikeRepository.existsByPostAndMember(post, member);
      return PostResponse.of(post, tags, alreadyLiked);
    });
  }

  // 특정 게시글 조회
  public PostResponse getPostById(Long postId) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 게시글 ID입니다."));

    Member member = memberService.getMember();
    boolean alreadyLiked = postLikeRepository.existsByPostAndMember(post, member);

    // 조회수 증가
    post.setViewCount(post.getViewCount() + 1);
    postRepository.save(post);

    // 태그 함께 조회
    List<Tag> tags = postTagRepository.findTagsByPostId(postId);

    PostResponse postResponse = PostResponse.of(post, tags, alreadyLiked);

    // CacheService에서 Double 타입의 평균 평점 가져오기
    Double averageRating = cacheService.getAverageRating(post.getDrink().getId());

    if (averageRating != null) {
      // 소수점 둘째 자리까지 평균 평점 조회
      String formattedRating = String.format("%.2f", averageRating);
      Double formattedAverageRating = Double.parseDouble(formattedRating);

      DrinkResponse drinkResponse = DrinkResponse.from(post.getDrink());
      drinkResponse.setAverageRating(formattedAverageRating);
      postResponse.setDrink(drinkResponse);
    }

    return postResponse;
  }

  // 좋아요 토글 로직
  @Transactional
  public void toggleLike(Long postId) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 게시글 ID입니다."));

    Member member = memberService.getMember();
    boolean alreadyLiked = postLikeRepository.existsByPostAndMember(post, member);

    if (alreadyLiked) {
      // 이미 좋아요 상태라면 좋아요 취소
      post.setLikeCount(post.getLikeCount() - 1);
      postLikeRepository.deleteByPostAndMember(post, member);
    } else {
      // 좋아요가 눌리지 않은 상태라면 좋아요 추가
      post.setLikeCount(post.getLikeCount() + 1);
      PostLike postLike = new PostLike();
      postLike.setPost(post);
      postLike.setMember(member);
      postLikeRepository.save(postLike);
    }

    postRepository.save(post);
  }

  @Transactional
  public void likePost(Long postId, boolean isLiked) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 게시글 ID입니다."));

    // 좋아요가 눌린 상태라면 좋아요 취소(좋아요 수 감소)
    if (isLiked) {
      post.setLikeCount(post.getLikeCount() - 1);
    } else {
      // 좋아요가 눌리지 않은 상태라면 좋아요 추가(좋아요 수 증가)
      post.setLikeCount(post.getLikeCount() + 1);
    }

    postRepository.save(post);
  }

  // 게시글 삭제
  public void deletePostById(Long postId) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 게시글 ID입니다."));

    Member member = memberService.getMember();

    //작성자 본인 또는 관리자만 글 삭제 가능
    if (!post.getMember().equals(member) && !member.getRole().equals(Role.MANAGER)) {
      throw new CustomException(ErrorCode.ACCESS_DENIED);
    }

    postRepository.deleteById(postId);
    searchService.delete(post);
    cacheManager.getCache("avg-rating").evict(post.getDrink().getId());
  }

  // 게시글 수정
  @Transactional
  @CacheEvict(value = "avg-rating", key = "#postRequest.drinkId", condition = "#postRequest.drinkId != null")
  public PostResponse updatePost(Long postId, PostRequest postRequest) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 게시글 ID입니다."));

    post.setContent(postRequest.getContent());
    post.setRating(postRequest.getRating());

    // 음료 정보 수정할 시
    if (postRequest.getDrinkId() != null) {
      Drink drink = drinkRepository.findById(postRequest.getDrinkId())
          .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 특산주입니다."));
      post.setDrink(drink);
    }

    if (postRequest.getMemberId() != null) {
      Member member = memberRepository.findById(postRequest.getMemberId())
          .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자입니다."));
      post.setMember(member);
    }

    // 이미지 수정: 1순위 : 게시글 업로드 이미지, 2순위 : 특산주 등록 이미지
    String imageUrl = postRequest.getImageUrl() != null ? postRequest.getImageUrl() : post.getDrink().getImageUrl();
    post.setImageUrl(imageUrl);

    post = postRepository.save(post);

    // 기존 태그 가져오기
    List<Tag> existingTags = postTagRepository.findTagsByPostId(postId);
    List<String> newTagNames = postRequest.getTag() != null ? postRequest.getTag() : List.of();

    // 기존 태그 중에서 새로운 요청에 없는 태그는 삭제
    for (Tag existingTag : existingTags) {
      if (!newTagNames.contains(existingTag.getTagName())) {
        PostTag postTag = postTagRepository.findByPostAndTag(post, existingTag)
            .orElse(null);
        if (postTag != null) {
          postTagRepository.delete(postTag);
        }
      }
    }

    // 새로운 태그를 추가
    for (String newTagName : newTagNames) {
      boolean alreadyExists = existingTags.stream().anyMatch(tag -> tag.getTagName().equals(newTagName));
      if (!alreadyExists) {
        Tag tag = tagRepository.findByTagName(newTagName)
            .orElseGet(() -> {
              Tag newTag = new Tag();
              newTag.setTagName(newTagName);
              autoCompleteService.saveAutoCompleteTag(newTagName);
              return tagRepository.save(newTag);
            });

        PostTag newPostTag = new PostTag(post, tag);
        postTagRepository.save(newPostTag);
      }
    }

    // 최종 업데이트된 태그 목록 가져오기
    List<Tag> updatedTags = postTagRepository.findTagsByPostId(postId);
    searchService.save(post, updatedTags);

    return PostResponse.of(post, updatedTags, false);
  }
}