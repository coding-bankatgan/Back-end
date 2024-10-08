package com.onedrinktoday.backend.domain.notification.controller;

import com.onedrinktoday.backend.domain.notification.dto.NotificationResponse;
import com.onedrinktoday.backend.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping("/notifications")
  public Page<NotificationResponse> getRecentNotifications(
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

    return notificationService.getRecentNotifications(pageable)
        .map(NotificationResponse::from);
  }

  @GetMapping("/notifications/{notificationId}")
  public NotificationResponse getNotification(@PathVariable Long notificationId) {

    return notificationService.getNotification(notificationId);
  }
}