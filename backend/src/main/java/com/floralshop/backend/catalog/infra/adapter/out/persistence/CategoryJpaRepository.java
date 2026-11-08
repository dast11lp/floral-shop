package com.floralshop.backend.catalog.infra.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

// Repositorio de Spring Data para categorías. Detalle de infraestructura.
interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, Long> {

    // Spring Data lo construye por el nombre: solo activas, por sortOrder y luego por nombre.
    List<CategoryJpaEntity> findByActiveTrueOrderBySortOrderAscNameAsc();
}