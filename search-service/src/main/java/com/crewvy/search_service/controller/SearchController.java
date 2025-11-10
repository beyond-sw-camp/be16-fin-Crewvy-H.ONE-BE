package com.crewvy.search_service.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.search_service.service.SearchService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/employees")
    public ResponseEntity<?> searchEmployees(@RequestParam String query,
                                             @RequestHeader("X-User-CompanyId") String companyId,
                                             Pageable pageable) {
        return new ResponseEntity<>(ApiResponse.success(
                searchService.searchEmployees(query, companyId, pageable), "직원 검색 성공"), HttpStatus.OK);
    }

    @GetMapping("/organization")
    public ResponseEntity<?> getOrganizationTree(@RequestHeader("X-User-CompanyId") String companyId) {
        return new ResponseEntity<>(ApiResponse.success(
                searchService.getOrganizationTree(companyId), "조직 검색 성공"), HttpStatus.OK);
    }

    @GetMapping("/employees/organization")
    public ResponseEntity<?> searchEmployeesByOrganization(@RequestParam String organizationId,
                                                           @RequestHeader("X-User-CompanyId") UUID companyId,
                                                           Pageable pageable) {
        return new ResponseEntity<>(ApiResponse.success(
                searchService.searchEmployeesByOrganization(organizationId, companyId, pageable), "조직별 직원 검색 성공"), HttpStatus.OK);
    }

    @GetMapping("/approvals")
    public ResponseEntity<?> searchApprovals(@RequestParam String query,
                                             @RequestHeader("X-User-MemberPositionId") String memberPositionId,
                                             Pageable pageable) {
        return new ResponseEntity<>(ApiResponse.success(
                searchService.searchApprovals(query, memberPositionId, pageable), "결재 문서 페이징 검색 성공"), HttpStatus.OK);
    }

    @GetMapping("/global")
    public ResponseEntity<?> searchGlobal(@RequestParam String query,
                                          @RequestHeader("X-User-CompanyId") String companyId,
                                          @RequestHeader("X-User-MemberPositionId") String memberPositionId) {
        return new ResponseEntity<>(ApiResponse.success(
                searchService.searchGlobal(query, companyId, memberPositionId), "통합 검색 성공"), HttpStatus.OK);
    }
}
