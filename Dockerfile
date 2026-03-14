# Etapa de construcción
FROM ubuntu:22.04 as builder

# Instalar dependencias necesarias
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    curl \
    tar \
    openjdk-17-jdk  # Java 17 como fallback en caso de problemas con Java 25

# Descargar Java 25 desde OpenJDK
RUN wget https://download.java.net/java/early_access/loom/25/binaries/jdk-25-ea+28_linux-x64_bin.tar.gz \
    && tar -xzf jdk-25-ea+28_linux-x64_bin.tar.gz \
    && mv jdk-25-ea+28 /opt/jdk-25

# Configuración de variables de entorno
ENV JAVA_HOME=/opt/jdk-25
ENV PATH="$JAVA_HOME/bin:$PATH"

# Etapa de construcción de la app
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Etapa de ejecución
FROM ubuntu:22.04

# Instalar dependencias de Java para ejecutar
RUN apt-get update && apt-get install -y \
    libglib2.0-0 \
    libxext6 \
    libxrender1 \
    libxtst6 \
    && rm -rf /var/lib/apt/lists/*

# Copiar el JDK de la etapa anterior
COPY --from=builder /opt/jdk-25 /opt/jdk-25

# Configurar el entorno
ENV JAVA_HOME=/opt/jdk-25
ENV PATH="$JAVA_HOME/bin:$PATH"

# Copiar el archivo JAR de la aplicación
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Exponer el puerto
EXPOSE 8080

# Iniciar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
