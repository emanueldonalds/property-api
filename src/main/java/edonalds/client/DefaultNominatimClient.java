package edonalds.client;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Primary
@Slf4j
public class DefaultNominatimClient implements NominatimClient {

    private final String url;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DefaultNominatimClient(
        @Value("${client.nominatim.url}") String url,
        ObjectMapper objectMapper) {

        this.url = url;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public NominatimResponse[] search(NominatimRequest request) {
        var uri = UriComponentsBuilder.fromUriString(url)
            .path("/search")
            .queryParam("street", request.street())
            .queryParam("county", request.county())
            .encode(StandardCharsets.UTF_8)
            .build()
            .toUri();

        var httpRequest = HttpRequest.newBuilder()
            .uri(uri)
            .header("User-Agent", "PropertyApiService/1.0")
            .build();

        try {
            var response = httpClient.send(httpRequest, BodyHandlers.ofInputStream());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), NominatimResponse[].class);
            }
            log.error("Nominatim returned http status {}", response.statusCode());
        } catch (IOException e) {
            log.error("Nominatim request failed.", e);
        } catch (InterruptedException e) {
            log.error("Nominatim request was interrupted.", e);
            Thread.currentThread().interrupt();
        }

        return new NominatimResponse[0];
    }
}
