package com.kosten.ai.service;

import com.kosten.ai.exception.AiServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiAiService {

    @Value("${google.ai.api-key}")
    private String apiKey;

    @Value("${google.ai.model}")
    private String model;

    @Value("${google.ai.api-url}")
    private String apiUrl;

    private final WebClient.Builder webClientBuilder;

    public String extractInvoiceData(String base64Image) {
        try {
            if (base64Image == null || base64Image.isEmpty()) {
                throw new AiServiceException("Base64 image data is empty");
            }
            
            WebClient webClient = webClientBuilder.baseUrl(apiUrl).build();

            String prompt = """
                Analysiere diese Rechnung und extrahiere die folgenden Informationen.
                Antworte NUR mit einem gültigen JSON-Objekt in diesem exakten Format (ohne zusätzlichen Text oder Markdown):
                {
                  "invoiceNumber": "die Rechnungsnummer",
                  "vendor": "Name der Firma oder des Anbieters",
                  "amount": "Gesamtbetrag als Zahl (nur Ziffern und Punkt, z.B. 150.50)",
                  "date": "Rechnungsdatum im Format YYYY-MM-DD",
                  "description": "kurze Beschreibung der Leistungen oder Produkte"
                }
                
                Wenn eine Information nicht gefunden wird, nutze diese Werte:
                - invoiceNumber: "Unbekannt"
                - vendor: "Unbekannt"
                - amount: "0"
                - date: aktuelles Datum
                - description: "Keine Beschreibung verfügbar"
                """;

            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of(
                        "parts", List.of(
                            Map.of("text", prompt),
                            Map.of("inline_data", Map.of(
                                "mime_type", "image/jpeg",
                                "data", base64Image
                            ))
                        )
                    )
                )
            );

            Mono<Map> response = webClient.post()
                .uri("/" + model + ":generateContent")
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(
                                        new AiServiceException("Gemini API error: " + errorBody))))
                .bodyToMono(Map.class);

            Map<String, Object> result = response.block();
            
            if (result != null && result.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) result.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (!parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }
            
            throw new AiServiceException("No valid response received from Gemini AI");
            
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                throw new AiServiceException("Gemini API rate limit exceeded. Please try again later", e);
            } else if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                throw new AiServiceException("Invalid Gemini API key or unauthorized access", e);
            }
            throw new AiServiceException("Gemini API request failed: " + e.getMessage(), e);
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("Unexpected error during invoice data extraction: " + e.getMessage(), e);
        }
    }

    public String analyzeInvoiceText(String invoiceText) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(apiUrl).build();

            String prompt = """
                Analysiere diesen Rechnungstext und extrahiere folgende Informationen im JSON Format:
                - Rechnungsnummer (invoiceNumber)
                - Anbieter/Firma (vendor)
                - Gesamtbetrag (amount)
                - Rechnungsdatum (date im Format YYYY-MM-DD)
                - Beschreibung der Leistungen (description)
                
                Text:
                """ + invoiceText;

            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", prompt)
                    ))
                )
            );

            Mono<Map> response = webClient.post()
                .uri("/" + model + ":generateContent")
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class);

            Map<String, Object> result = response.block();
            
            if (result != null && result.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) result.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (!parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }
            
            return "Fehler bei der Analyse";
            
        } catch (Exception e) {
            return "Fehler: " + e.getMessage();
        }
    }
}
