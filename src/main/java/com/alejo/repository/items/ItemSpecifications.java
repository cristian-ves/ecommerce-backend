package com.alejo.repository.items;

import com.alejo.entities.items.Item;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class ItemSpecifications {

    private ItemSpecifications() {
    }

    public static Specification<Item> search(String query, List<Integer> categoryIds) {
        return (root, cq, cb) -> {
            var predicate = cb.and(cb.isTrue(root.get("accepted")));

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.toLowerCase() + "%";
                predicate = cb.and(
                        predicate,
                        cb.or(
                                cb.like(cb.lower(root.get("name")), pattern),
                                cb.like(cb.lower(root.get("description")), pattern)
                        )
                );
            }

            if (categoryIds != null && !categoryIds.isEmpty()) {
                predicate = cb.and(predicate, root.get("category").get("id").in(categoryIds));
            }

            return predicate;
        };
    }
}