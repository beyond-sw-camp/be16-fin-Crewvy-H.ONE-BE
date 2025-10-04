package com.crewvy.workspace_service.meeting.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workspace_service.meeting.constant.VideoConferenceStatus;
import com.crewvy.workspace_service.meeting.dto.VideoConferenceBookRes;
import com.crewvy.workspace_service.meeting.dto.VideoConferenceCreateReq;
import com.crewvy.workspace_service.meeting.dto.VideoConferenceCreateRes;
import com.crewvy.workspace_service.meeting.dto.VideoConferenceListRes;
import com.crewvy.workspace_service.meeting.service.VideoConferenceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/video-conferences")
public class VideoConferenceController {

    private final VideoConferenceService videoConferenceService;

    public VideoConferenceController(VideoConferenceService videoConferenceService) {
        this.videoConferenceService = videoConferenceService;
    }

    @PostMapping("")
    public ResponseEntity<?> createVideoConference(@RequestParam(name = "immediate", required = false, defaultValue = "false") boolean immediate,
                                                   @RequestBody VideoConferenceCreateReq videoConferenceCreateReq) {
        if (immediate) {
            VideoConferenceCreateRes res = videoConferenceService.createVideoConference(videoConferenceCreateReq);
            return new ResponseEntity<>(ApiResponse.success(res, "화상회의 생성 성공"), HttpStatus.CREATED);
        } else {
            VideoConferenceBookRes res = videoConferenceService.bookVideoConference(videoConferenceCreateReq);
            return new ResponseEntity<>(ApiResponse.success(res, "화상회의 등록 성공"), HttpStatus.CREATED);
        }
    }

    @GetMapping("")
    public ResponseEntity<?> findAllMyVideoConferences(/*@RequestHeader("X-Member-Id") UUID memberId,*/
                                                       @RequestParam("status") VideoConferenceStatus status,
                                                       @PageableDefault(value = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<VideoConferenceListRes> res = videoConferenceService.findAllMyVideoConference(null, status, pageable);
        return new ResponseEntity<>(ApiResponse.success(res, "나의 화상회의 조회 성공"), HttpStatus.OK);
    }
}
