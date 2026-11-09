package cl.duoc.cluodnative.documents;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record CreateDocumentRequest(
        @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,50}") String clientId,
        @NotBlank @Size(max=80) String clientName,
        @NotBlank @Size(max=120) String concept,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull LocalDate date) {}
