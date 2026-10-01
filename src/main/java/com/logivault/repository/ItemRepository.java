package com.logivault.repository;

import com.logivault.dto.item.ItemSummary;
import com.logivault.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {

    @EntityGraph(attributePaths = "variants")
    Optional<Item> findWithVariantsById(UUID id);

    @Query(value = """
            select new com.logivault.dto.item.ItemSummary(
                i.id, i.name, i.basePrice, i.active, count(v.id), coalesce(sum(v.stock), 0L), i.createdAt)
            from Item i left join i.variants v
            where (:q is null or lower(i.name) like :q)
              and (:active is null or i.active = :active)
            group by i.id, i.name, i.basePrice, i.active, i.createdAt
            """,
            countQuery = """
            select count(i) from Item i
            where (:q is null or lower(i.name) like :q)
              and (:active is null or i.active = :active)
            """)
    Page<ItemSummary> search(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);
}
