//package com.crewvy.workspace_service.meeting.controller;
//
//import com.crewvy.common.dto.ApiResponse;
//import com.crewvy.workspace_service.meeting.constant.VideoConferenceStatus;
//import com.crewvy.workspace_service.meeting.dto.*;
//import com.crewvy.workspace_service.meeting.service.VideoConferenceService;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.data.web.PageableDefault;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/video-conferences")
//public class OldVideoConferenceController {
//
//    private final VideoConferenceService videoConferenceService;
//
//    public OldVideoConferenceController(VideoConferenceService videoConferenceService) {
//        this.videoConferenceService = videoConferenceService;
//    }
//
//    @PostMapping("")
//    public ResponseEntity<?> createVideoConference(@RequestParam(name = "immediate", required = false, defaultValue = "false") boolean immediate,
//                                                   @RequestHeader("X-User-UUID") UUID memberId,
//                                                   @RequestBody VideoConferenceCreateReq videoConferenceCreateReq) {
//        if (immediate) {
//            OpenViduSessionRes res = videoConferenceService.createVideoConference(memberId, videoConferenceCreateReq);
//            return new ResponseEntity<>(ApiResponse.success(res, "화상회의 생성 성공"), HttpStatus.CREATED);
//        } else {
//            VideoConferenceBookRes res = videoConferenceService.bookVideoConference(memberId, videoConferenceCreateReq);
//            return new ResponseEntity<>(ApiResponse.success(res, "화상회의 등록 성공"), HttpStatus.CREATED);
//        }
//    }
//
//    @PostMapping("/{videoConferenceId}/join")
//    public ResponseEntity<?> joinVideoConference(@RequestHeader("X-User-UUID") UUID memberId,
//                                                 @PathVariable("videoConferenceId") UUID videoConferenceId) {
//        OpenViduSessionRes res = videoConferenceService.joinVideoConference(memberId, videoConferenceId);
//        return new ResponseEntity<>(ApiResponse.success(res, "화상회의 참여 성공"), HttpStatus.OK);
//    }
//
//    @PostMapping("/{videoConferenceId}/start")
//    public ResponseEntity<?> startVideoConference(@RequestHeader("X-User-UUID") UUID memberId,
//                                                  @PathVariable("videoConferenceId") UUID videoConferenceId) {
//        OpenViduSessionRes res = videoConferenceService.startVideoConference(memberId, videoConferenceId);
//        return new ResponseEntity<>(ApiResponse.success(res, "화상회의 시작 성공"), HttpStatus.OK);
//    }
//
//    @GetMapping("")
//    public ResponseEntity<?> findAllMyVideoConferences(@RequestHeader("X-User-UUID") UUID memberId,
//                                                        @RequestParam("status") VideoConferenceStatus status,
//                                                        @PageableDefault(value = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
//        Page<VideoConferenceListRes> res = videoConferenceService.findAllMyVideoConference(memberId, status, pageable);
//        return new ResponseEntity<>(ApiResponse.success(res, "나의 화상회의 조회 성공"), HttpStatus.OK);
//    }
//
//    @PutMapping("/{videoConferenceId}")
//    public ResponseEntity<?> updateVideoConference(@RequestHeader("X-User-UUID") UUID memberId,
//                                                   @PathVariable("videoConferenceId") UUID videoConferenceId,
//                                                   @RequestBody VideoConferenceUpdateReq videoConferenceUpdateReq) {
//        VideoConferenceUpdateRes res = videoConferenceService.updateVideoConference(memberId, videoConferenceId, videoConferenceUpdateReq);
//        return new ResponseEntity<>(ApiResponse.success(res, "화상회의 수정 성공"), HttpStatus.OK);
//    }
//
//    @DeleteMapping("/{videoConferenceId}")
//    public ResponseEntity<?> deleteVideoConference(@RequestHeader("X-User-UUID") UUID memberId,
//                                                   @PathVariable("videoConferenceId") UUID videoConferenceId) {
//
//        videoConferenceService.deleteVideoConference(memberId, videoConferenceId);
//        return new ResponseEntity<>(ApiResponse.success(null, "화상회의 취소 성공"), HttpStatus.OK);
//    }
//
//    @PostMapping("/{videoConferenceId}/messages")
//    public ResponseEntity<?> sendMessage(@RequestHeader("X-User-UUID") UUID memberId,
//                                         @PathVariable UUID videoConferenceId,
//                                         @RequestBody ChatMessageReq chatMessageReq) {
//        videoConferenceService.sendMessage(memberId, videoConferenceId, chatMessageReq);
//        return ResponseEntity.ok().build();
//    }
//
//    @GetMapping("/{videoConferenceId}/messages")
//    public ResponseEntity<?> findMessages(@RequestHeader("X-User-UUID") UUID memberId,
//                                          @PathVariable UUID videoConferenceId,
//                                          @PageableDefault(value = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
//
//        Page<ChatMessageRes> res = videoConferenceService.findMessages(memberId, videoConferenceId, pageable);
//        return new ResponseEntity<>(ApiResponse.success(res, "채팅 목록 조회 성공"), HttpStatus.OK);
//    }
//}
