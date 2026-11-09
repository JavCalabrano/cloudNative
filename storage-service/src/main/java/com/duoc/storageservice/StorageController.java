package com.duoc.storageservice;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/internal/storage")
public class StorageController {

    private final StorageService service;

    public StorageController(StorageService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> store(
            @RequestPart("file") MultipartFile file,
            @RequestParam String clientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) throws IOException {
        return service.store(file, clientId, date);
    }

    @GetMapping("/{clientId}/{date}/{filename:.+}")
    public ResponseEntity<byte[]> download(
            @PathVariable String clientId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String filename) throws IOException {
        byte[] data = service.load(clientId, date, filename);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(data);
    }
}