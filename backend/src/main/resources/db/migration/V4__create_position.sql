CREATE TABLE position (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  campaign_id     INTEGER NOT NULL REFERENCES campaign(id),
  opening_leg_id  INTEGER REFERENCES trade_leg(id),
  instrument_type TEXT    NOT NULL,
  ticker          TEXT    NOT NULL,
  option_type     TEXT,
  strike          REAL,
  expiry          DATE,
  open_action     TEXT    NOT NULL,
  open_quantity   INTEGER NOT NULL,
  avg_price       REAL    NOT NULL,
  status          TEXT    NOT NULL DEFAULT 'OPEN',
  opened_at       DATE    NOT NULL,
  closed_at       DATE
);
