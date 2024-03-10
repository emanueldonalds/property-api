package edonalds.model;

import java.util.ArrayList;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ListingsQuery(
        Long id,
        ArrayList<String> agency,
        Boolean deleted,
        @Min(value = 1, message = "Min limit is 1") @Max(value = 1000, message = "Max limit is 1000") Integer limit,
        Integer minPrice,
        Integer maxPrice,
        Integer minSize,
        Integer maxSize,
        Integer minRooms,
        Integer maxRooms,
        String orderByDesc,
        String orderByAsc) {

    public ListingsQuery {
        if (deleted == null) {
            deleted = false;
        }
        if (limit == null) {
            limit = 1000;
        }
        if (orderByDesc == null && orderByAsc == null) {
            orderByDesc = "firstSeen";
        }
    }
}
