# --- ЭТАП СБОРКИ (Build Stage) ---
# Используем базовый образ с JDK 11 и Gradle для сборки проекта
FROM gradle:7.6.0-jdk11 AS build

# Указываем рабочую директорию внутри контейнера
WORKDIR /home/gradle/project

# Копируем все файлы проекта в рабочую директорию
COPY . .

# Выполняем сборку проекта с помощью Gradle
# Это создаст .jar файл в build/libs/
RUN gradle clean build --no-daemon

# --- ЭТАП ЗАПУСКА (Runtime Stage) ---
# Используем минимальный базовый образ с Java 11 для экономии места
FROM eclipse-temurin:11-jre-jammy

# Указываем рабочую директорию для приложения
WORKDIR /app

# Копируем ТОЛЬКО собранный .jar файл из этапа сборки
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar

# Команда, которая будет выполняться при запуске контейнера
ENTRYPOINT ["java", "-jar", "app.jar"]
