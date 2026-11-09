package cl.duoc.cloudnative.documents;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
@RestController @RequestMapping("/api/documents")
public class DocumentController {
    private final PdfGenerator generator; private final RestClient storage;
    public DocumentController(PdfGenerator generator,@Value("${storage.service.url:http://localhost:8081}") String url) {
        this.generator=generator;this.storage=RestClient.builder().baseUrl(url).build();
    }
    @PostMapping public ResponseEntity<Map> create(@Valid @RequestBody CreateDocumentRequest input) throws IOException {
        byte[] pdf=generator.generate(input);
        String filename="documento-"+UUID.randomUUID()+".pdf";
        ByteArrayResource resource=new ByteArrayResource(pdf){@Override public String getFilename(){return filename;}};
        MultiValueMap<String,Object> form=new LinkedMultiValueMap<>();
        form.add("file",resource);form.add("clientId",input.clientId());form.add("date",input.date().toString());
        Map result=storage.post().uri("/internal/storage").contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form).retrieve().body(Map.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    @GetMapping("/{clientId}/{date}/{filename:.+}") public ResponseEntity<byte[]> download(
            @PathVariable String clientId,@PathVariable String date,@PathVariable String filename) {
        byte[] file=storage.get().uri(b->b.path("/internal/storage/{clientId}/{date}/{filename}").build(clientId,date,filename))
                .retrieve().body(byte[].class);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+filename+"\"").body(file);
    }
}
