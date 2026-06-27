package com.demoapp.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.demoapp.demo.model.UserPostReaction;
import com.demoapp.demo.repository.UserPostReactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostService {

  private final UserPostReactionRepository reactionRepository;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  public PostService(UserPostReactionRepository reactionRepository) {
    this.reactionRepository = reactionRepository;
    this.restTemplate = new RestTemplate();
    this.objectMapper = new ObjectMapper();
  }

  public Map<String, Object> getPosts(Integer limit, Integer skip, Long userId) {
    try {
      StringBuilder url = new StringBuilder("https://dummyjson.com/posts");
      List<String> params = new ArrayList<>();
      
      if (limit != null) params.add("limit=" + limit);
      if (skip != null) params.add("skip=" + skip);

      if (!params.isEmpty()) {
        url.append("?").append(String.join("&", params));
      }

      String response = restTemplate.getForObject(url.toString(), String.class);
      JsonNode rootNode = objectMapper.readTree(response);

      Set<Long> likedPostIds = new HashSet<>();
      Set<Long> dislikedPostIds = new HashSet<>();

      if (userId != null) {
        likedPostIds = reactionRepository.findByUserIdAndReactionType(userId, "like")
          .stream().map(UserPostReaction::getPostId).collect(Collectors.toSet());

        dislikedPostIds = reactionRepository.findByUserIdAndReactionType(userId, "dislike")
          .stream().map(UserPostReaction::getPostId).collect(Collectors.toSet());
      }

      List<Map<String, Object>> posts = new ArrayList<>();
      JsonNode postsArray = rootNode.get("posts");
      
      for (JsonNode postNode : postsArray) {
        Map<String, Object> post = new HashMap<>();
        Long postId = postNode.get("id").asLong();
        
        post.put("id", postId);
        post.put("title", postNode.get("title").asText());
        post.put("body", postNode.get("body").asText());
        post.put("liked", likedPostIds.contains(postId));
        post.put("disliked", dislikedPostIds.contains(postId));

        // Reactions counts from DummyJSON
        if (postNode.has("reactions")) {
          JsonNode reactionsNode = postNode.get("reactions");
          Map<String, Object> reactions = new HashMap<>();
          reactions.put("likes", reactionsNode.has("likes") ? reactionsNode.get("likes").asInt() : 0);
          reactions.put("dislikes", reactionsNode.has("dislikes") ? reactionsNode.get("dislikes").asInt() : 0);
          post.put("reactions", reactions);
        }
        
        posts.add(post);
      }

      Map<String, Object> result = new HashMap<>();
      result.put("posts", posts);
      result.put("total", rootNode.get("total").asInt());
      result.put("skip", rootNode.get("skip").asInt());
      result.put("limit", rootNode.get("limit").asInt());

      return result;

    } catch (Exception e) {
      throw new RuntimeException("Erro ao buscar posts: " + e.getMessage(), e);
    }
  }

  public Map<String, Object> getLikedPosts(Long userId, Integer limit, Integer skip) {
    try {
      if (limit == null) limit = 5;
      if (skip == null) skip = 0;

      List<UserPostReaction> allLikes = reactionRepository.findByUserIdAndReactionType(userId, "like");
      
      List<Long> likedPostIds = allLikes.stream()
        .map(UserPostReaction::getPostId)
        .collect(Collectors.toList());

      int total = likedPostIds.size();
      int fromIndex = Math.min(skip, total);
      int toIndex = Math.min(skip + limit, total);
      List<Long> paginatedIds = likedPostIds.subList(fromIndex, toIndex);

      List<Map<String, Object>> posts = new ArrayList<>();
      
      for (Long postId : paginatedIds) {
        String url = "https://dummyjson.com/posts/" + postId;
        String response = restTemplate.getForObject(url, String.class);
        JsonNode postNode = objectMapper.readTree(response);
        
        Map<String, Object> post = new HashMap<>();
        post.put("id", postNode.get("id").asLong());
        post.put("title", postNode.get("title").asText());
        post.put("body", postNode.get("body").asText());
        post.put("liked", true);
        post.put("disliked", false);
        
        posts.add(post);
      }

      Map<String, Object> result = new HashMap<>();
      result.put("posts", posts);
      result.put("total", total);
      result.put("skip", skip);
      result.put("limit", limit);

      return result;

    } catch (Exception e) {
      throw new RuntimeException("Erro ao buscar posts curtidos: " + e.getMessage(), e);
    }
  }

  public Map<String, Object> toggleLike(Long postId, Long userId) {
    Optional<UserPostReaction> existingDislike =
        reactionRepository.findByUserIdAndPostIdAndReactionType(userId, postId, "dislike");
    existingDislike.ifPresent(reactionRepository::delete);

    Optional<UserPostReaction> existingLike =
        reactionRepository.findByUserIdAndPostIdAndReactionType(userId, postId, "like");

    boolean liked;
    if (existingLike.isPresent()) {
      reactionRepository.delete(existingLike.get());
      liked = false;
    } else {
      UserPostReaction reaction = new UserPostReaction();
      reaction.setUserId(userId);
      reaction.setPostId(postId);
      reaction.setReactionType("like");
      reactionRepository.save(reaction);
      liked = true;
    }

    Map<String, Object> result = new HashMap<>();
    result.put("postId", postId);
    result.put("liked", liked);
    result.put("disliked", false);
    
    return result;
  }

  public Map<String, Object> toggleDislike(Long postId, Long userId) {
    Optional<UserPostReaction> existingLike =
        reactionRepository.findByUserIdAndPostIdAndReactionType(userId, postId, "like");
    existingLike.ifPresent(reactionRepository::delete);

    Optional<UserPostReaction> existingDislike =
        reactionRepository.findByUserIdAndPostIdAndReactionType(userId, postId, "dislike");

    boolean disliked;
    if (existingDislike.isPresent()) {
      reactionRepository.delete(existingDislike.get());
      disliked = false;
    } else {
      UserPostReaction reaction = new UserPostReaction();
      reaction.setUserId(userId);
      reaction.setPostId(postId);
      reaction.setReactionType("dislike");
      reactionRepository.save(reaction);
      disliked = true;
    }

    Map<String, Object> result = new HashMap<>();
    result.put("postId", postId);
    result.put("liked", false);
    result.put("disliked", disliked);
    
    return result;
  }

}
