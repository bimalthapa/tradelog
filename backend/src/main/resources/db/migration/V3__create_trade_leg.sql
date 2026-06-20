CREATE TABLE trade_leg (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  trade_entry_id  INTEGER NOT NULL REFERENCES trade_entry(id),
  campaign_id     INTEGER NOT NULL REFERENCES campaign(id),
  instrument_type TEXT    NOT NULL,
  action          TEXT    NOT NULL,
  ticker          TEXT    NOT NULL,
  quantity        INTEGER NOT NULL,
  price           REAL    NOT NULL,
  net_cash_flow   REAL    NOT NULL,
  option_type     TEXT,
  strike          REAL,
  expiry          DATE,
  closes_leg_id   INTEGER REFERENCES trade_leg(id),
  traded_at       DATE    NOT NULL
);
