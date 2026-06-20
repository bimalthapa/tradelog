CREATE TABLE campaign (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  ticker       TEXT    NOT NULL,
  label        TEXT,
  status       TEXT    NOT NULL DEFAULT 'OPEN',
  notes        TEXT,
  opened_at    DATE    NOT NULL,
  closed_at    DATE,
  realized_pnl REAL
);
