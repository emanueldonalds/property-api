package edonalds.client;

public interface NominatimClient {
    NominatimResponse[] search(NominatimRequest request);
}
