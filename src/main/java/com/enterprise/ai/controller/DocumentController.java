package com.enterprise.ai.controller;

import com.enterprise.ai.dto.IngestionResponse;
import com.enterprise.ai.model.AccessLevel;
import com.enterprise.ai.service.DocumentIngestionService;
import com.enterprise.ai.service.VectorStoreService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentIngestionService ingestionService;
    private final VectorStoreService vectorStoreService;

    public DocumentController(DocumentIngestionService ingestionService, VectorStoreService vectorStoreService) {
        this.ingestionService = ingestionService;
        this.vectorStoreService = vectorStoreService;
    }

    public record TextUploadRequest(String documentName, String content, String accessLevel) {}

    @PostMapping("/upload-text")
    public ResponseEntity<IngestionResponse> uploadText(@RequestBody TextUploadRequest request) {
        AccessLevel level = AccessLevel.fromString(request.accessLevel());
        IngestionResponse response = ingestionService.ingestText(request.documentName(), request.content(), level);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/upload-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IngestionResponse> uploadPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "accessLevel", defaultValue = "PUBLIC") String accessLevel) throws IOException {

        AccessLevel level = AccessLevel.fromString(accessLevel);
        IngestionResponse response = ingestionService.ingestPdf(file, level);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
                "totalIndexedChunks", vectorStoreService.getTotalIndexedChunks(),
                "status", "ONLINE",
                "vectorStore", "In-Memory Normalized Dense Vector Index"
        ));
    }
}

