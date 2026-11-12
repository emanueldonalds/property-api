package edonalds.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edonalds.model.GetListingsResponse;
import edonalds.model.Listing;
import edonalds.model.ListingsQuery;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/listings")
@Slf4j
public class ListingController {
    private final String apiKey;
    private final ListingService listingService;
    private final GeoLocationService geoLocationService;

    public ListingController(
        ListingService listingService,
        GeoLocationService geoLocationService,
        @Value("${security.apiKey}") String apiKey
    ) {
        this.listingService = listingService;
        this.apiKey = apiKey;
        this.geoLocationService = geoLocationService;
    }

    @GetMapping
    public GetListingsResponse getListings(@Valid ListingsQuery query) {
        return listingService.getListings(query);
    }


    @PutMapping
    public ResponseEntity<Void> updateListings(
        @RequestHeader(value = "x-api-key", required = false) String apiKeyHeader,
        @RequestBody List<Listing> listingsRequest) {

        if (apiKeyHeader == null || apiKeyHeader.isBlank() || !apiKeyHeader.equals(apiKey)) {
            return ResponseEntity.status(401).build();
        }

        var listings = listingService.updateListings(listingsRequest);
        geoLocationService.addGeoLocations(listings);

        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, List<String>>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
            .stream().map(FieldError::getDefaultMessage).collect(Collectors.toList());

        Map<String, List<String>> errorResponse = new HashMap<>();
        errorResponse.put("errors", errors);

        return new ResponseEntity<>(errorResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }
}
