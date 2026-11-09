package com.duoc.storageservice;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Service
public class StorageService {

    private final S3Client s3;
    private final String bucket;
    private final Path efsRoot;

    public StorageService(S3Client s3,
                          @Value("${storage.s3.bucket}") String bucket,
                          @Value("${storage.efs.path:/mnt/efs}") String efsPath) {
        this.s3 = s3;
        this.bucket = bucket;
        this.efsRoot = Path.of(efsPath).toAbsolutePath().normalize();
    }

    public Map<String, String> store(MultipartFile file, String clientId, LocalDate date) throws IOException {
        checkId(clientId);
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PDF vacío o demasiado grande");
        }
        byte[] header = file.getInputStream().readNBytes(5);
        if (!Arrays.equals(header, new byte[]{'%', 'P', 'D', 'F', '-'})) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requiere PDF");
        }

        String name = "documento-" + UUID.randomUUID() + ".pdf";
        String yyyy = String.valueOf(date.getYear());
        String mm = String.format("%02d", date.getMonthValue());
        String dd = String.format("%02d", date.getDayOfMonth());

        // Ruta en EFS: {efsRoot}/{clientId}/{yyyy}/{MM}/{dd}/{filename}
        Path efsDir = efsRoot.resolve(clientId).resolve(yyyy).resolve(mm).resolve(dd);
        Files.createDirectories(efsDir);
        Path efsFile = efsDir.resolve(name);

        // Key en S3: clientes/{clientId}/{yyyy}/{MM}/{dd}/{filename}
        String s3Key = "clientes/" + clientId + "/" + yyyy + "/" + mm + "/" + dd + "/" + name;

        try (InputStream in = file.getInputStream()) {
            // 1) Escribir en EFS (persistente)
            Files.copy(in, efsFile, StandardCopyOption.REPLACE_EXISTING);

            // 2) Subir a S3 desde EFS
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(s3Key)
                            .contentType("application/pdf")
                            .build(),
                    RequestBody.fromFile(efsFile)
            );
        } catch (IOException e) {
            Files.deleteIfExists(efsFile);
            throw e;
        } catch (RuntimeException e) {
            Files.deleteIfExists(efsFile);
            throw e;
        }

        return Map.of(
                "clientId", clientId,
                "date", date.toString(),
                "filename", name,
                "s3Key", s3Key,
                "efsPath", efsFile.toString(),
                "storage", "S3+EFS"
        );
    }

    public byte[] load(String clientId, LocalDate date, String filename) throws IOException {
        checkId(clientId);
        if (!filename.matches("documento-[a-f0-9-]{36}\\.pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre inválido");
        }

        String yyyy = String.valueOf(date.getYear());
        String mm = String.format("%02d", date.getMonthValue());
        String dd = String.format("%02d", date.getDayOfMonth());

        Path efsFile = efsRoot.resolve(clientId).resolve(yyyy).resolve(mm).resolve(dd).resolve(filename);
        if (Files.exists(efsFile)) {
            return Files.readAllBytes(efsFile);
        }

        // Fallback: S3
        String s3Key = "clientes/" + clientId + "/" + yyyy + "/" + mm + "/" + dd + "/" + filename;
        try {
            byte[] data = s3.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(s3Key).build()
            ).asByteArray();

            // Rehidratar EFS (best-effort)
            try {
                Files.createDirectories(efsFile.getParent());
                Files.write(efsFile, data);
            } catch (IOException ignored) { /* no bloquea la descarga */ }

            return data;
        } catch (NoSuchKeyException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado");
        }
    }

    private void checkId(String id) {
        if (!id.matches("[A-Za-z0-9_-]{1,50}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cliente inválido");
        }
    }
}