package com.crewvy.workspace_service.meeting.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workspace_service.meeting.constant.VideoConferenceStatus;
import com.crewvy.workspace_service.meeting.dto.*;
import com.crewvy.workspace_service.meeting.dto.ai.TranscribeReq;
import com.crewvy.workspace_service.meeting.dto.ai.TranscribeRes;
import com.crewvy.workspace_service.meeting.entity.Recording;
import com.crewvy.workspace_service.meeting.repository.RecordingRepository;
import com.crewvy.workspace_service.meeting.service.VideoConferenceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/video-conferences")
public class VideoConferenceController {

    private final VideoConferenceService videoConferenceService;
    private final RecordingRepository recordingRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> meetingKafkaTemplate;

    public VideoConferenceController(VideoConferenceService videoConferenceService, RecordingRepository recordingRepository, ObjectMapper objectMapper, KafkaTemplate<String, String> meetingKafkaTemplate) {
        this.videoConferenceService = videoConferenceService;
        this.recordingRepository = recordingRepository;
        this.objectMapper = objectMapper;
        this.meetingKafkaTemplate = meetingKafkaTemplate;
    }

    @PostMapping("")
    public ResponseEntity<?> createVideoConference(@RequestParam(name = "immediate", required = false, defaultValue = "false") boolean immediate,
                                                   @RequestHeader("X-User-UUID") UUID memberId,
                                                   @RequestHeader("X-User-Name") String name,
                                                   @RequestBody VideoConferenceCreateReq videoConferenceCreateReq) {

        if (immediate) {
            String decodedName = URLDecoder.decode(name, StandardCharsets.UTF_8);
            LiveKitSessionRes res = videoConferenceService.createVideoConference(memberId, decodedName, videoConferenceCreateReq);
            return new ResponseEntity<>(ApiResponse.success(res, "화상회의 생성 성공"), HttpStatus.CREATED);
        } else {
            VideoConferenceBookRes res = videoConferenceService.bookVideoConference(memberId, videoConferenceCreateReq);
            return new ResponseEntity<>(ApiResponse.success(res, "화상회의 등록 성공"), HttpStatus.CREATED);
        }
    }

    @PostMapping("/{videoConferenceId}/join")
    public ResponseEntity<?> joinVideoConference(@RequestHeader("X-User-UUID") UUID memberId,
                                                 @RequestHeader("X-User-Name") String name,
                                                 @PathVariable("videoConferenceId") UUID videoConferenceId) {
        String decodedName = URLDecoder.decode(name, StandardCharsets.UTF_8);
        LiveKitSessionRes res = videoConferenceService.joinVideoConference(memberId, decodedName, videoConferenceId);
        return new ResponseEntity<>(ApiResponse.success(res, "화상회의 참여 성공"), HttpStatus.OK);
    }

    @PostMapping("/{videoConferenceId}/start")
    public ResponseEntity<?> startVideoConference(@RequestHeader("X-User-UUID") UUID memberId,
                                                  @RequestHeader("X-User-Name") String name,
                                                  @PathVariable("videoConferenceId") UUID videoConferenceId) {

        String decodedName = URLDecoder.decode(name, StandardCharsets.UTF_8);
        LiveKitSessionRes res = videoConferenceService.startVideoConference(memberId, decodedName, videoConferenceId);
        return new ResponseEntity<>(ApiResponse.success(res, "화상회의 시작 성공"), HttpStatus.OK);
    }

    @GetMapping("")
    public ResponseEntity<?> findAllMyVideoConferences(@RequestHeader("X-User-UUID") UUID memberId,
                                                       @RequestParam("status") VideoConferenceStatus status,
                                                       @PageableDefault(value = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<VideoConferenceListRes> res = videoConferenceService.findAllMyVideoConference(memberId, status, pageable);
        return new ResponseEntity<>(ApiResponse.success(res, "나의 화상회의 조회 성공"), HttpStatus.OK);
    }

    @PutMapping("/{videoConferenceId}")
    public ResponseEntity<?> updateVideoConference(@RequestHeader("X-User-UUID") UUID memberId,
                                                   @PathVariable("videoConferenceId") UUID videoConferenceId,
                                                   @RequestBody VideoConferenceUpdateReq videoConferenceUpdateReq) {

        VideoConferenceUpdateRes res = videoConferenceService.updateVideoConference(memberId, videoConferenceId, videoConferenceUpdateReq);
        return new ResponseEntity<>(ApiResponse.success(res, "화상회의 수정 성공"), HttpStatus.OK);
    }

    @DeleteMapping("/{videoConferenceId}")
    public ResponseEntity<?> deleteVideoConference(@RequestHeader("X-User-UUID") UUID memberId,
                                                   @PathVariable("videoConferenceId") UUID videoConferenceId) {

        videoConferenceService.deleteVideoConference(memberId, videoConferenceId);
        return new ResponseEntity<>(ApiResponse.success(null, "화상회의 취소 성공"), HttpStatus.OK);
    }

    @PostMapping("/{videoConferenceId}/messages")
    public ResponseEntity<?> sendMessage(@RequestHeader("X-User-UUID") UUID memberId,
                                         @PathVariable UUID videoConferenceId,
                                         @RequestBody ChatMessageReq chatMessageReq) {

        videoConferenceService.sendMessage(memberId, videoConferenceId, chatMessageReq);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{videoConferenceId}/messages")
    public ResponseEntity<?> findMessages(@RequestHeader("X-User-UUID") UUID memberId,
                                          @PathVariable UUID videoConferenceId,
                                          @PageableDefault(value = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ChatMessageRes> res = videoConferenceService.findMessages(memberId, videoConferenceId, pageable);
        return new ResponseEntity<>(ApiResponse.success(res, "채팅 목록 조회 성공"), HttpStatus.OK);
    }

    @PostMapping("/{videoConferenceId}/minutes")
    public ResponseEntity<?> saveMinute(@PathVariable UUID videoConferenceId,
                                        @RequestBody TranscribeRes transcribeRes) {

        return new ResponseEntity<>(ApiResponse.success("회의록 저장 성공"), HttpStatus.CREATED);
    }

    @GetMapping("/{videoConferenceId}/minutes")
    public ResponseEntity<?> findMinute(@PathVariable UUID videoConferenceId) {
        MinuteRes res = videoConferenceService.findMinute(videoConferenceId);
        return new ResponseEntity<>(ApiResponse.success(res, "회의록 조회 성공"), HttpStatus.OK);
    }

    @PostMapping("/{videoConferenceId}/passwords")
    public ResponseEntity<?> findPassword(@RequestHeader("X-User-UUID") UUID memberId,
                                           @PathVariable UUID videoConferenceId) {
        VideoConferencePasswordRes res = videoConferenceService.findPassword(memberId, videoConferenceId);
        return new ResponseEntity<>(ApiResponse.success(res, "화상회의 비밀번호 발급 또는 조회 성공"), HttpStatus.OK);
    }

    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestHeader("X-User-UUID") UUID memberId,
                                  @RequestHeader("X-User-Name") String name,
                                  @RequestBody VideoConferenceJoinReq videoConferenceJoinReq) {
        String decodedName = URLDecoder.decode(name, StandardCharsets.UTF_8);
        LiveKitSessionRes res = videoConferenceService.joinVideoConferenceWithPassword(memberId, decodedName, videoConferenceJoinReq);
        return new ResponseEntity<>(ApiResponse.success(res, "화상회의 참여 성공"), HttpStatus.OK);
    }

    @PostMapping("/test/{recordingId}")
    public ResponseEntity<?> test(@PathVariable UUID recordingId) {
        Recording recording = recordingRepository.findById(recordingId).orElseThrow(() -> new EntityNotFoundException("?"));

        TranscribeReq transcribeReq = TranscribeReq.fromEntity(recording);

        String data;
        try {
            data = objectMapper.writeValueAsString(transcribeReq);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("오류 발생");
        }

        meetingKafkaTemplate.send("transcribe-request", data);
        return ResponseEntity.ok().build();
    }
}
