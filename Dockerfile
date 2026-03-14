# Etapa de construcción
FROM ubuntu:22.04 as builder

# Instalar dependencias necesarias
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    curl \
    openjdk-17-jdk # Instalar JDK 17 como fallback en caso de problemas con Java 25

# Descargar e instalar Java 25 (usando el enlace de acceso anticipado de OpenJDK)
RUN wget https://download.java.net/java/early_access/loom/25/binaries/jdk-25-ea+28_linux-x64_bin.tar.gz \
    && tar -xzf jdk-25-ea+28_linux-x64_bin.tar.gz \
    && mv jdk-25-ea+28 /opt/jdk-25

# Configurar las variables de entorno para usar Java 25
ENV JAVA_HOME=/opt/jdk-25
ENV PATH="$JAVA_HOME/bin:$PATH"

# Etapa de construcción de la app
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Etapa de ejecución
FROM ubuntu:22.04

# Instalar dependencias de Java para la ejecución
RUN apt-get update && apt-get install -y \
    libglib2.0-0 \
    libxext6 \
    libxrender1 \
    libxtst6 \
    && rm -rf /var/lib/apt/lists/*

# Copiar el JDK de la etapa anterior
COPY --from=builder /opt/jdk-25 /opt/jdk-25

# Configurar las variables de entorno para usar Java 25
ENV JAVA_HOME=/opt/jdk-25
ENV PATH="$JAVA_HOME/bin:$PATH"

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
