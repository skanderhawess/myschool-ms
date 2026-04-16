package com.myschool.userservice.client;

import com.myschool.userservice.client.dto.NotificationRequestDto;
import com.myschool.userservice.client.dto.NotificationResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Client Feign pour communiquer avec notification-service via Eureka.
 */
@FeignClient(name = "NOTIFICATION-SERVICE")
public interface NotificationClient {

    @PostMapping("/notifications/send")
    NotificationResponseDto sendNotification(@RequestBody NotificationRequestDto request);
}
