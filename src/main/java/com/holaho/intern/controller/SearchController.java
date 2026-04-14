package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.elasticsearch.model.InternIndex;
import com.holaho.intern.shared.elasticsearch.model.TaskIndex;
import com.holaho.intern.shared.elasticsearch.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/interns")
    public ResponseEntity<ApiResponse<List<InternIndex>>> searchInterns(@RequestParam String query) {
        List<InternIndex> results = searchService.searchInterns(query);
        return ResponseEntity.ok(ApiResponse.success("Search successful", results));
    }

    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<List<TaskIndex>>> searchTasks(@RequestParam String query) {
        List<TaskIndex> results = searchService.searchTasks(query);
        return ResponseEntity.ok(ApiResponse.success("Search successful", results));
    }
}

