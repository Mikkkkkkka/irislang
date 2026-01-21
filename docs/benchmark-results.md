# Тесты производительности IRIS VM

## Обзор

Ниже приведены два теста, используемые для оценки влияния JIT-компиляции и сборщика мусора на поведение VM.

---

## Тест 1: JIT-компиляция

**Файл:** `modules/vm/src/test/kotlin/dev/iris/vm/JitBenchmarkTest.kt`

**Программа:** вызов функции `fibonacci(10)` в цикле 500 раз.

### Результат

```
=== JIT PERFORMANCE COMPARISON ===
Program: Calling fibonacci(10) 500 times

WITHOUT JIT: 97ms (interpreted)
WITH JIT:    10ms (compiled after 10 calls)
Speedup:     9.70x

JIT compilation provides significant speedup
```

### Комментарии

- без JIT все вызовы выполняются в интерпретаторе;
- с JIT функция компилируется после 10 вызовов;
- в данном тесте зафиксировано ускорение около 9.7x.

---

## Тест 2: Сборка мусора

**Файл:** `modules/vm/src/test/kotlin/dev/iris/vm/GcBenchmarkTest.kt`

**Программа:** создание 300 массивов, большинство из которых становятся недостижимыми.

### Результат

```
=== GARBAGE COLLECTION DEMONSTRATION ===
Program: Creates 300 arrays, most become garbage

WITHOUT frequent GC:
  Threshold: 100000 (very high)
  GC runs: ~0 times
  Heap objects: 301 (accumulated)

WITH frequent GC:
  Threshold: 50 (low)
  GC runs: ~6 times
  Heap objects: 2 (cleaned up)

GC freed 299 objects from heap
Memory reduction: 301 -> 2 objects
```

### Комментарии

- при высоком пороге GC объекты накапливаются;
- при низком пороге сборщик запускается чаще;
- в данном тесте количество объектов в куче снизилось с 301 до 2.

---

## Итоги

- JIT-компиляция может существенно ускорять горячие функции при повторных вызовах.
- Сборщик мусора уменьшает число объектов в куче при достаточной частоте запусков.

---

## Как запустить

```bash
# Оба теста
./gradlew :vm:test --tests "*BenchmarkTest"

# Только JIT
./gradlew :vm:test --tests "JitBenchmarkTest"

# Только GC
./gradlew :vm:test --tests "GcBenchmarkTest"
```
