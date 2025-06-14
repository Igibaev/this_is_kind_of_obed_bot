# --- ЭТАП СБОРКИ (Build Stage) ---
# Используем базовый образ с JDK 11 и Gradle 8.8
FROM gradle:8.8-jdk11 AS build

# Указываем рабочую директорию внутри контейнера
WORKDIR /home/gradle/project

# Копируем все файлы проекта в рабочую директорию
COPY . .

# Выполняем сборку проекта. Так как в build.gradle указано имя для shadowJar,
# нам не нужны дополнительные манипуляции.
RUN gradle clean build -x test --no-daemon

# --- ЭТАП ЗАПУСКА (Runtime Stage) ---
# Используем минимальный базовый образ с Java 11 для экономии места
FROM eclipse-temurin:11-jre-jammy

# Указываем рабочую директорию для приложения
WORKDIR /app

# Копируем ТОЛЬКО итоговый fat jar, имя которого задано в build.gradle
COPY --from=build /home/gradle/project/build/libs/food_bot_telegram.jar app.jar

# Команда, которая будет выполняться при запуске контейнера
ENTRYPOINT ["java", "-jar", "app.jar"]
