package com.floralshop.backend;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

// Pruebas de arquitectura: hacen cumplir el ADR 0001 y su enmienda, el 0009.
// Leen el código ya compilado. No necesitan Spring ni base de datos.
class ArchitectureTest {

    private static JavaClasses mainClasses;

    @BeforeAll
    static void importClasses() {
        // Solo el código de producción: se excluyen las clases de prueba.
        mainClasses = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.floralshop.backend");
    }

    // Regla 1: el dominio no conoce frameworks ni las otras capas.
    @Test
    void domainDoesNotDependOnFrameworksOrOtherLayers() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "software.amazon..",
                        "..application..",
                        "..infra..")
                .because("el dominio debe ser Java puro (ADR 0001, regla 1)")
                .check(mainClasses);
    }

    // Regla 2: application solo depende de domain (y de Java estándar).
    @Test
    void applicationDependsOnlyOnDomain() {
        classes().that().resideInAPackage("..application..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "..application..",
                        "..domain..",
                        "java..")
                .because("application solo puede usar domain (ADR 0001, regla 2)")
                .check(mainClasses);
    }

    // Regla 3: nada del núcleo mira hacia infra. Ya la garantizan las dos anteriores;
    // se deja explícita para que el mensaje de error, si falla, sea más claro.
    @Test
    void domainAndApplicationNeverDependOnInfra() {
        noClasses().that().resideInAnyPackage("..domain..", "..application..")
                .should().dependOnClassesThat().resideInAPackage("..infra..")
                .because("infra depende del núcleo, nunca al revés (ADR 0001, regla 3)")
                .check(mainClasses);
    }
}