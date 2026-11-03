# ---------- Etapa 1: construir el .jar ----------
# Usa el JDK completo (compilador). Esta etapa se descarta al final.
FROM eclipse-temurin:21-jdk AS build
#creamos el directorio de trabajo y entramos en el
WORKDIR /app 

# Primero solo lo necesario para bajar dependencias.
# Docker guarda esta capa en cache mientras el pom.xml no cambie,
# así los builds siguientes no vuelven a descargar todo Maven.
# copiamos desde el dorectorio backend (donde está el Dockerfile) al directorio de trabajo del contenedor /app, la carpeeta -mv, por ende que da /app/.mvn y el mvnw 
COPY .mvn .mvn 
#Aquí estamos copiando dos archivos: el ejecutable de Maven Wrapper, y el pom.xml a la raiz del directorio de trabajo actual /app equivale a ./
COPY mvnw pom.xml ./
# ejecutar mvnw desde la posición actual (./) para descargar dependencias y plugins de Maven, sin compilar ni testear.
RUN ./mvnw -B dependency:go-offline 
# Después copia el código fuente, que es lo que cambia más seguido.
COPY src src
RUN ./mvnw -B package -DskipTests

# ---------- Etapa 2: imagen final ----------
# Solo el JRE (para ejecutar, no compilar): imagen más chica. (no incluye todo el jdk)
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuario sin privilegios: la app no corre como root.
# agregamos el usuario, que sea de sistema, para correr servicios y aplicaciones nada personal
# no cree un home, y el nombre del usuario es spring
# Por seguridad, evitar que Spring Boot se ejecute con permisos de administrador (root).
RUN useradd --system --no-create-home spring
# a partir de acá, los comandos se ejecutan como el usuario spring, no root, (no elimina root)
USER spring


#indica que el archivo se copia desde la etapa llamada build (la primera). desde ahllí copia el archivo .jar que se encuentra 
# en /app/target/ y lo copia al directorio de trabajo actual (que es /app) con el nombre app.jar
COPY --from=build /app/target/*.jar app.jar

# solo es un metadato informativo, no tiene efecto en la ejecución del contenedor. Indica que el contenedor escucha en el puerto 8080.
EXPOSE 8080

# MaxRAMPercentage hace que la JVM (objetos) use hasta el 75 % de la memoria del contenedor, java necesita memoria para el heap y para la JVM, si no se limita, puede usar toda la memoria del contenedor y el contenedor se reinicia.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]