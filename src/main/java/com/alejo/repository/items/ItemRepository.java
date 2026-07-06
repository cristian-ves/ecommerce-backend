package com.alejo.repository.items;

import com.alejo.entities.items.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends PagingAndSortingRepository<Item, Integer>,
        JpaSpecificationExecutor<Item> {

    Page<Item> findAllByAcceptedTrueAndUser_SuspendedFalse(Pageable pageable);

    @Query("""
    SELECT i FROM Item i
    WHERE (LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(i.description) LIKE LOWER(CONCAT('%', :query, '%')))
      AND i.accepted = true
""")
    List<Item> search(@Param("query") String query);

    List<Item> findByCategoryIdInAndAcceptedTrue(List<Integer> ids);

    Optional<Item> findById(int id);

    void save(Item item);

    List<Item> findByUser_Id(int id);

    List<Item> findAllByAcceptedFalseAndRejectedFalse();

    @Query("""
        SELECT i.user, COUNT(i) as totalItems
        FROM Item i
        GROUP BY i.user
        ORDER BY totalItems DESC
    """)
    List<Object[]> findTopUsersByItems();

}