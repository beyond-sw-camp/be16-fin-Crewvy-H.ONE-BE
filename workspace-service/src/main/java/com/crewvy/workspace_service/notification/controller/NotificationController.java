package com.crewvy.workspace_service.notification.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workspace_service.notification.dto.request.NotificationSettingReqDto;
import com.crewvy.workspace_service.notification.dto.response.NotificationResDto;
import com.crewvy.workspace_service.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
public class NotificationController {
    private final NotificationService notificationService;


    @GetMapping("/get-my-alarm")
    public ResponseEntity<?> getMyAlarm(@RequestHeader("X-User-UUID") UUID memberId) {
        List<NotificationResDto> dtoList = notificationService.getMyAlarm(memberId);
        return new ResponseEntity<>(ApiResponse.success(dtoList, "내 알림 조회"), HttpStatus.OK);
    }

    @PatchMapping("/read/{id}")
    public ResponseEntity<?> readAlarm(@PathVariable UUID id) {
        notificationService.readAlarm(id);
        return new ResponseEntity<>(ApiResponse.success(id, "알림 읽음"), HttpStatus.OK);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<?> readAll(@RequestHeader("X-User-UUID") UUID memberId) {
        notificationService.readAll(memberId);
        return new ResponseEntity<>(
                ApiResponse.success(memberId, "전체 읽음"),
                HttpStatus.OK
        );
    }

    @GetMapping("/find-my-setting")
    public ResponseEntity<?> findMySetting(@RequestHeader("X-User-UUID") UUID memberId) {
        return new ResponseEntity<>(
                ApiResponse.success(notificationService.findMySetting(memberId), "내 알림 설정 조회"),
                HttpStatus.OK
        );
    }

    @PatchMapping("/update-setting")
    public ResponseEntity<?> updateSetting(@RequestBody List<NotificationSettingReqDto> dtoList) {
        notificationService.updateSetting(dtoList);
        return new ResponseEntity<>(
                ApiResponse.success(null, "알림 설정 변경"),
                HttpStatus.OK
        );
    }
}
