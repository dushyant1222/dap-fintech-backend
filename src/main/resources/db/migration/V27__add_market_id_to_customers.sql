ALTER TABLE customers
ADD COLUMN market_id UUID;

ALTER TABLE customers
ADD CONSTRAINT fk_customer_market
FOREIGN KEY (market_id)
REFERENCES markets(id);