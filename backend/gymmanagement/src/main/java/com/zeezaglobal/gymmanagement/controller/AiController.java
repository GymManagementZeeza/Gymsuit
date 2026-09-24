package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.AiSummaryResponse;
import com.zeezaglobal.gymmanagement.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/summarize")
    public AiSummaryResponse summarize(@RequestBody Object data) {
        String summary = aiService.summarize(data);
        return new AiSummaryResponse(summary);
    }
}
