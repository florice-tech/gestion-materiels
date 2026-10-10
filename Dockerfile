# Étape 1 : Construction du projet avec Maven et Java 21
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copier les fichiers du wrapper Maven et du projet pour le cache des dépendances
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Télécharger les dépendances hors-ligne
RUN ./mvnw dependency:go-offline

# Copier le reste du code source
COPY . .

# Compiler et empaqueter l'application en fichier .jar
RUN ./mvnw clean package -DskipTests

# Étape 2 : Exécution de l'application légère
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copier le .jar généré depuis l'étape de construction
COPY --from=build /app/target/*.jar app.jar

# Exposer le port de l'application Spring Boot
EXPOSE 8080

# Lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]