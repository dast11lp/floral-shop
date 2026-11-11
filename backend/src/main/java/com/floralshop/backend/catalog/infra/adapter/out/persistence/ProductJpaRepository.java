package com.floralshop.backend.catalog.infra.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, Long> {


    Page<ProductJpaEntity> findByActiveTrue(Pageable pageable);

    @Query(value = """
            select p from ProductJpaEntity p
            join p.categories c
            where p.active = true and c.slug = :slug and c.active = true
            """,
            countQuery = """
            select count(p) from ProductJpaEntity p
            join p.categories c
            where p.active = true and c.slug = :slug and c.active = true
            """)
    Page<ProductJpaEntity> findActiveByCategorySlug(@Param("slug") String slug, Pageable pageable);

    Optional<ProductJpaEntity> findBySlugAndActiveTrue(String slug);

    // Una sola consulta y cuatro columnas por variante. v.active va explícito: no se depende
    // de que el @SQLRestriction de la colección se aplique dentro de un join.
    @Query("""
            select v.id as variantId, v.preparationHours as variantPreparationHours,
                   p.preparationHours as productPreparationHours, v.slotUnits as slotUnits
            from ProductJpaEntity p join p.variants v
            where p.active = true and v.active = true and v.id in :variantIds
            """)
    List<VariantWorkloadRow> findAvailableWorkloads(@Param("variantIds") Collection<Long> variantIds);
}