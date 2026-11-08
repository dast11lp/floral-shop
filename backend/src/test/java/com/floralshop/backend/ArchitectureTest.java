package com.floralshop.backend;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import jakarta.persistence.Entity;

// Pruebas de arquitectura: hacen cumplir el ADR 0001 y sus enmiendas (0009 y 0010).
// Leen el código ya compilado. No necesitan Spring ni base de datos.
class ArchitectureTest {

    private static final String BASE = "com.floralshop.backend";

    private static JavaClasses mainClasses;
    private static Set<String> modules;

    @BeforeAll
    static void importClasses() {
        // Solo el código de producción: se excluyen las clases de prueba.
        mainClasses = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE);

        // Los módulos se descubren solos: cada paquete que cuelga directamente de BASE
        // (catalog, scheduling, health...). Un módulo nuevo queda comprobado sin tocar nada.
        modules = mainClasses.stream()
                .map(JavaClass::getPackageName)
                .filter(name -> name.startsWith(BASE + "."))
                .map(name -> name.substring(BASE.length() + 1).split("\\.")[0])
                .collect(Collectors.toSet());
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

    // Regla 4: un módulo solo usa la API pública de otro (application.service y domain).
    // Lo interno es infra y application.port: ningún otro módulo debe tocarlo.
    @Test
    void modulesOnlyUseTheApiOfOtherModules() {
        for (String module : modules) {
            String[] internalsOfOthers = modules.stream()
                    .filter(other -> !other.equals(module))
                    .flatMap(other -> Stream.of(
                            BASE + "." + other + ".infra..",
                            BASE + "." + other + ".application.port.."))
                    .toArray(String[]::new);

            noClasses().that().resideInAPackage(BASE + "." + module + "..")
                    .should().dependOnClassesThat().resideInAnyPackage(internalsOfOthers)
                    .because("el módulo '" + module + "' solo puede usar la API pública de los otros (ADR 0001, regla 4)")
                    .check(mainClasses);
        }
    }

    // Regla 5 (parte 1): las entidades JPA solo viven en el adapter de persistencia de su módulo.
    @Test
    void entitiesLiveOnlyInPersistenceAdapters() {
        classes().that().areAnnotatedWith(Entity.class)
                .should().resideInAPackage("..infra.adapter.out.persistence..")
                .because("las tablas son de su módulo: solo su adapter de persistencia las toca (ADR 0001, regla 5)")
                .check(mainClasses);
    }

    // Regla 5 (parte 2): los repositorios de Spring Data no son públicos, para que nadie fuera
    // de su paquete pueda leer las tablas de otro módulo.
    @Test
    void springDataRepositoriesAreNotPublic() {
        classes().that().areAssignableTo(JpaRepository.class)
                .should().notBePublic()
                .because("un repositorio público dejaría a otros módulos leer tablas ajenas (ADR 0001, regla 5)")
                .check(mainClasses);
    }

    // Regla 6: no hay ciclos entre módulos.
    @Test
    void modulesHaveNoCycles() {
        slices().matching(BASE + ".(*)..")
                .should().beFreeOfCycles()
                .because("los módulos no pueden depender unos de otros en círculo (ADR 0001, regla 6)")
                .check(mainClasses);
    }
}