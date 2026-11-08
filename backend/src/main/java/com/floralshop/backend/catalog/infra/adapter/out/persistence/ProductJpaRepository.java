package com.floralshop.backend.catalog.infra.adapter.out.persistence;

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
}