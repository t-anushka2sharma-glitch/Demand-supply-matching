# Demand-Supply Matching Engine (Java Exercise 1)

An online market-maker matching engine for farmer (supply) and customer (demand)
produce orders.

Currently implemented:
- **Part 1** — in-memory matching engine, reads orders from stdin, writes trades to stdout.
- **Part 2 [MySQL]** — same engine, but orders and trades are persisted to MySQL via plain JDBC.

## Matching rules implemented

1. Priority is given to "lower supply price – higher demand price" matching (maximizes the market maker's spread).
2. The supplier always gets the price they asked for, regardless of what the demand side offered.
3. Within the same price, first-in-first-out (by arrival order) applies.
4. A trade is generated whenever `demand price >= supply price`, and is recorded at the **supply's** price.
5. Unmatched (or partially matched) orders stay in the ledger indefinitely — no expiry.

## Project layout

```
src/main/java/com/marketmaker/
  model/       Order, Trade, OrderType, OrderStatus
  engine/      MatchingEngine — the core matching logic (in-memory ledger)
  cli/         OrderLineParser, Part1Main, Part2Main — stdin/stdout drivers
  db/          DbConnectionManager, OrderDao, TradeDao — plain-JDBC persistence (Part 2)
src/main/resources/
  schema.sql       MySQL DDL for the orders/trades tables
  db.properties    JDBC connection settings (edit this for your local MySQL)
src/test/java/...  MatchingEngineTest — reproduces both worked examples from the spec
```

## Prerequisites

- JDK 8+
- Maven 3.6+
- MySQL 5.7+/8.x (only needed for Part 2)

## Part 1 — run the in-memory engine

```bash
mvn compile exec:java -Dexec.mainClass="com.marketmaker.cli.Part1Main" < input.txt
```

Or interactively:

```bash
mvn compile exec:java -Dexec.mainClass="com.marketmaker.cli.Part1Main"
# then type orders one per line, e.g.
# s1 09:45 tomato 24/kg 100kg
# d1 09:47 tomato 22/kg 110kg
# Ctrl-D to end input
```

Run the unit tests (which check both worked examples from the spec byte-for-byte):

```bash
mvn test
```

## Part 2 — run with MySQL persistence

1. Create the schema:

   ```bash
   mysql -u root -p < src/main/resources/schema.sql
   ```

2. Edit `src/main/resources/db.properties` with your MySQL username/password
   (defaults to `root`/`root` on `localhost:3306`).

3. Run:

   ```bash
   mvn compile exec:java -Dexec.mainClass="com.marketmaker.cli.Part2Main" < input.txt
   ```

   Input still comes from the command line/stdin exactly as in Part 1 — only the
   storage layer changed. Every order and trade is written to the `orders` and
   `trades` tables as it happens, and on restart any still-open orders are
   reloaded into the ledger so matching can continue across runs.

4. Verify in MySQL:

   ```sql
   USE demand_supply_db;
   SELECT * FROM orders;
   SELECT * FROM trades;
   ```

## Verifying against the spec's worked examples

Both examples from the exercise PDF are encoded as JUnit tests in
`MatchingEngineTest`, and were also hand-traced against the algorithm above —
the engine's output matches expected output exactly:

```
d1 s2 20/kg 90kg
d1 s3 19/kg 20kg
d2 s3 19/kg 10kg
d3 s3 19/kg 20kg
```

and

```
d2 s1 110/kg 1kg
d2 s2 110/kg 7kg
d2 s3 110/kg 2kg
d1 s4 110/kg 1kg
d3 s4 110/kg 10kg
```
