package com.floralshop.backend.catalog.infra.adapter.out.persistence;

import java.time.Instant;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

// Refleja la tabla product, con sus variantes y sus categorías.
// Las colecciones se cargan de forma perezosa y solo con lo activo, en el orden pedido.
@Entity
@Table(name = "product")
public class ProductJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "preparation_hours", nullable = false)
    private int preparationHours;

    // Las rellena la base con now(): Hibernate nunca las escribe.
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    // Uno a muchos: la columna product_id vive en la tabla product_variant.
    // SQLRestriction: la tienda solo ve las variantes activas.
    @OneToMany
    @JoinColumn(name = "product_id")
    @SQLRestriction("active = true")
    @OrderBy("sortOrder ASC")
    private List<ProductVariantJpaEntity> variants;

    // Muchos a muchos, a través de la tabla product_category. Solo categorías activas.
    @ManyToMany
    @JoinTable(name = "product_category",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    @SQLRestriction("active = true")
    @OrderBy("sortOrder ASC, name ASC")
    private List<CategoryJpaEntity> categories;

    protected ProductJpaEntity() {
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public int getPreparationHours() { return preparationHours; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<ProductVariantJpaEntity> getVariants() { return variants; }
    public List<CategoryJpaEntity> getCategories() { return categories; }
}