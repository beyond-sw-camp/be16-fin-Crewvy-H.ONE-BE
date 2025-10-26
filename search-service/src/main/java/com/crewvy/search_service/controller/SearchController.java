package com.crewvy.search_service.controller;

import com.crewvy.search_service.entity.MemberDocument;
import com.crewvy.search_service.entity.OrganizationDocument;
import com.crewvy.search_service.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/employees/search")
    public List<MemberDocument> searchEmployees(@RequestParam String query, @RequestHeader("X-User-CompanyId") String companyId) {
        return searchService.searchEmployees(query, companyId);
    }

    @GetMapping("/organization")
    public List<OrganizationDocument> getOrganizationTree(@RequestHeader("X-User-CompanyId") String companyId) {
        return searchService.getOrganizationTree(companyId);
    }
}
