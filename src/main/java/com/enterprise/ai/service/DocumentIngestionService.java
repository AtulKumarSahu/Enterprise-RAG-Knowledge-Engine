package com.enterprise.ai.service;

import com.enterprise.ai.dto.IngestionResponse;
import com.enterprise.ai.model.AccessLevel;
import com.enterprise.ai.model.DocumentChunk;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    @Value("${app.ai.rag.chunk-size:500}")
    private int targetChunkSize;

    @Value("${app.ai.rag.chunk-overlap:50}")
    private int chunkOverlap;

    public DocumentIngestionService(EmbeddingService embeddingService, VectorStoreService vectorStoreService) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }

    public IngestionResponse ingestText(String documentName, String textContent, AccessLevel accessLevel) {
        if (textContent == null || textContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Document content cannot be empty");
        }

        String documentId = UUID.randomUUID().toString();
        List<DocumentChunk> chunks = chunkText(documentId, documentName, textContent, 1, accessLevel);
        vectorStoreService.saveChunks(chunks);

        int totalTokens = chunks.stream().mapToInt(DocumentChunk::tokenCount).sum();
        log.info("[INGESTION] Ingested text document '{}' with {} chunks (access level: {})", documentName, chunks.size(), accessLevel);

        return new IngestionResponse(
                documentId,
                documentName,
                accessLevel.name(),
                chunks.size(),
                totalTokens,
                "Document parsed and indexed successfully"
        );
    }

    public IngestionResponse ingestPdf(MultipartFile file, AccessLevel accessLevel) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded PDF file is empty");
        }

        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf";
        String documentId = UUID.randomUUID().toString();
        List<DocumentChunk> allChunks = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);

                if (pageText != null && !pageText.trim().isEmpty()) {
                    List<DocumentChunk> pageChunks = chunkText(documentId, fileName, pageText, page, accessLevel);
                    allChunks.addAll(pageChunks);
                }
            }
        }

        vectorStoreService.saveChunks(allChunks);
        int totalTokens = allChunks.stream().mapToInt(DocumentChunk::tokenCount).sum();
        log.info("[INGESTION] Ingested PDF '{}' with {} chunks across pages (access level: {})", fileName, allChunks.size(), accessLevel);

        return new IngestionResponse(
                documentId,
                fileName,
                accessLevel.name(),
                allChunks.size(),
                totalTokens,
                "PDF extracted, chunked, and indexed successfully"
        );
    }

    private List<DocumentChunk> chunkText(String docId, String docName, String text, int pageNumber, AccessLevel accessLevel) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String normalized = text.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n").trim();
        int length = normalized.length();

        if (length <= targetChunkSize) {
            float[] embedding = embeddingService.generateEmbedding(normalized);
            int tokens = estimateTokens(normalized);
            chunks.add(new DocumentChunk(UUID.randomUUID().toString(), docId, docName, normalized, embedding, accessLevel, pageNumber, tokens));
            return chunks;
        }

        int start = 0;
        while (start < length) {
            int end = Math.min(start + targetChunkSize, length);

            // Avoid cutting words in half
            if (end < length) {
                int lastSpace = normalized.lastIndexOf(' ', end);
                if (lastSpace > start + (targetChunkSize / 2)) {
                    end = lastSpace;
                }
            }

            String chunkText = normalized.substring(start, end).trim();
            if (!chunkText.isEmpty()) {
                float[] embedding = embeddingService.generateEmbedding(chunkText);
                int tokens = estimateTokens(chunkText);
                chunks.add(new DocumentChunk(UUID.randomUUID().toString(), docId, docName, chunkText, embedding, accessLevel, pageNumber, tokens));
            }

            if (end >= length) {
                break;
            }

            start = Math.max(end - chunkOverlap, start + 1);
        }

        return chunks;
    }

    private int estimateTokens(String text) {
        return Math.max(1, text.split("\\s+").length);
    }
}

