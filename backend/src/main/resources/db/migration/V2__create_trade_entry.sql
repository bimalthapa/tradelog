CREATE TABLE trade_entry (
  id           INTEGER  PRIMARY KEY AUTOINCREMENT,
  campaign_id  INTEGER  NOT NULL REFERENCES campaign(id),
  entered_at   DATETIME NOT NULL,
  raw_input    TEXT,
  notes        TEXT,
  strategy_tag TEXT
);
