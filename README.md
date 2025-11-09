# Product Catalog Service

## Сборка проекта

1. Клонируйте репозиторий:

```bash
git clone https://github.com/garmax-5/Homework_Y_LAB.git
cd Homework_Y_LAB/product-catalog
```

2. Соберите проект с помощью Maven:

```bash
mvn clean install
```

Команда `clean install` выполняет очистку предыдущих сборок, компиляцию исходного кода, запуск тестов и упаковку проекта в JAR.

---

## Запуск проекта

### Через Maven

```bash
mvn exec:java -Dexec.mainClass="com.marketplace.in.ConsoleApp"
```

### Через сгенерированный JAR

```bash
mvn package
java -cp target/product-catalog-1.0-SNAPSHOT.jar com.marketplace.in.ConsoleApp
```

---

## Инструменты сборки

* **Maven** — сборка проекта, управление зависимостями и запуск.
