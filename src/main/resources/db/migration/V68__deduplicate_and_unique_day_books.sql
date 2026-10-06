-- Deduplicate market_day_book and day_book tables, and add UNIQUE constraints to prevent duplicate daybook entries.

-- 1. Point daybook_transactions to the winning market_day_book record (latest updated/created)
WITH duplicates AS (
    SELECT id, market_id, date,
           ROW_NUMBER() OVER (
               PARTITION BY market_id, date 
               ORDER BY updated_at DESC NULLS LAST, created_at DESC NULLS LAST, id DESC
           ) as rn,
           FIRST_VALUE(id) OVER (
               PARTITION BY market_id, date 
               ORDER BY updated_at DESC NULLS LAST, created_at DESC NULLS LAST, id DESC
           ) as winner_id
    FROM market_day_book
)
UPDATE daybook_transactions dt
SET market_daybook_id = d.winner_id
FROM duplicates d
WHERE dt.market_daybook_id = d.id AND d.rn > 1;

-- 2. Delete redundant duplicate market_day_book rows
WITH duplicates AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY market_id, date 
               ORDER BY updated_at DESC NULLS LAST, created_at DESC NULLS LAST, id DESC
           ) as rn
    FROM market_day_book
)
DELETE FROM market_day_book
WHERE id IN (SELECT id FROM duplicates WHERE rn > 1);

-- 3. Point daybook_transactions to the winning day_book record (latest updated/created)
WITH duplicates AS (
    SELECT id, employee_id, date,
           ROW_NUMBER() OVER (
               PARTITION BY employee_id, date 
               ORDER BY updated_at DESC NULLS LAST, created_at DESC NULLS LAST, id DESC
           ) as rn,
           FIRST_VALUE(id) OVER (
               PARTITION BY employee_id, date 
               ORDER BY updated_at DESC NULLS LAST, created_at DESC NULLS LAST, id DESC
           ) as winner_id
    FROM day_book
)
UPDATE daybook_transactions dt
SET daybook_id = d.winner_id
FROM duplicates d
WHERE dt.daybook_id = d.id AND d.rn > 1;

-- 4. Delete redundant duplicate day_book rows
WITH duplicates AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY employee_id, date 
               ORDER BY updated_at DESC NULLS LAST, created_at DESC NULLS LAST, id DESC
           ) as rn
    FROM day_book
)
DELETE FROM day_book
WHERE id IN (SELECT id FROM duplicates WHERE rn > 1);

-- 5. Add UNIQUE indexes to guarantee uniqueness per market/employee per date
CREATE UNIQUE INDEX IF NOT EXISTS uq_market_day_book_market_date ON market_day_book (market_id, date);
CREATE UNIQUE INDEX IF NOT EXISTS uq_day_book_employee_date ON day_book (employee_id, date);
