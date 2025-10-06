package com.crewvy.workspace_service.meeting.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workspace_service.meeting.constant.VideoConferenceStatus;
import com.crewvy.workspace_service.meeting.dto.*;
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
            /*@RequestHeader("X-Member-Id") UUID memberId,*/
                                                   @RequestBody VideoConferenceCreateReq videoConferenceCreateReq) {
        if (immediate) {
            OpenViduSessionRes res = videoConferenceService.createVideoConference(videoConferenceCreateReq);
            return new ResponseEntity<>(ApiResponse.success(res, "화상회의 생성 성공"), HttpStatus.CREATED);
        } else {
            VideoConferenceBookRes res = videoConferenceService.bookVideoConference(videoConferenceCreateReq);
            return new ResponseEntity<>(ApiResponse.success(res, "화상회의 등록 성공"), HttpStatus.CREATED);
        }
    }

    @PostMapping("/{videoConferenceId}/join")
    public ResponseEntity<?> joinVideoConference(/*@RequestHeader("X-Member-Id") UUID memberId,*/@PathVariable("videoConferenceId") UUID videoConferenceId) {
        OpenViduSessionRes res = videoConferenceService.joinVideoConference(videoConferenceId);
        return new ResponseEntity<>(ApiResponse.success(res, "화상회의 참여 성공"), HttpStatus.OK);
    }

    @PostMapping("/{videoConferenceId}/start")
    public ResponseEntity<?> startVideoConference(/*@RequestHeader("X-Member-Id") UUID memberId,*/@PathVariable("videoConferenceId") UUID videoConferenceId) {
        OpenViduSessionRes res = videoConferenceService.startVideoConference(videoConferenceId);
        return new ResponseEntity<>(ApiResponse.success(res, "화상회의 시작 성공"), HttpStatus.OK);
    }

    @GetMapping("")
    public ResponseEntity<?> findAllMyVideoConferences(/*@RequestHeader("X-Member-Id") UUID memberId,*/
                                                        @RequestParam("status") VideoConferenceStatus status,
                                                        @PageableDefault(value = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<VideoConferenceListRes> res = videoConferenceService.findAllMyVideoConference(null, status, pageable);
        return new ResponseEntity<>(ApiResponse.success(res, "나의 화상회의 조회 성공"), HttpStatus.OK);
    }

    @PutMapping("/{videoConferenceId}")
    public ResponseEntity<?> updateVideoConference(/*@RequestHeader("X-Member-Id") UUID memberId,*/
                                                    @PathVariable("videoConferenceId") UUID videoConferenceId,
                                                    @RequestBody VideoConferenceUpdateReq videoConferenceUpdateReq) {
        VideoConferenceUpdateRes res = videoConferenceService.updateVideoConference(videoConferenceId, videoConferenceUpdateReq);
        return new ResponseEntity<>(ApiResponse.success(res, "화상회의 수정 성공"), HttpStatus.OK);
    }

    @DeleteMapping("/{videoConferenceId}")
    public ResponseEntity<?> deleteVideoConference(/*@RequestHeader("X-Member-Id") UUID memberId,*/
                                                    @PathVariable("videoConferenceId") UUID videoConferenceId) {

        videoConferenceService.deleteVideoConference(videoConferenceId);
        return new ResponseEntity<>(ApiResponse.success(null, "화상회의 취소 성공"), HttpStatus.OK);
    }
}
