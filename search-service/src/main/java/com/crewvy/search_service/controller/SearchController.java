package com.crewvy.search_service.controller;

import com.crewvy.search_service.entity.MemberDocument;
import com.crewvy.search_service.service.MemberSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SearchController {

    private final MemberSearchService memberSearchService;

    public SearchController(MemberSearchService memberSearchService) {
        this.memberSearchService = memberSearchService;
    }

    @GetMapping("/unified")
    public List<MemberDocument> unifiedSearch(@RequestParam String keyword) {
        return memberSearchService.unifiedSearch(keyword);
    }

    @GetMapping("/autocomplete")
    public List<String> autocomplete(@RequestParam String keyword) {
        return memberSearchService.getAutocompleteSuggestions(keyword);
    }

    @GetMapping("/employees/search")
    public List<MemberDocument> searchEmployees(@RequestParam String query) {
        return memberSearchService.searchEmployees(query);
    }
}
