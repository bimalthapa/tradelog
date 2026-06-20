CREATE INDEX idx_trade_leg_campaign ON trade_leg(campaign_id);
CREATE INDEX idx_trade_leg_closes   ON trade_leg(closes_leg_id);
CREATE INDEX idx_position_campaign  ON position(campaign_id);
CREATE INDEX idx_position_status    ON position(status);
CREATE INDEX idx_campaign_ticker    ON campaign(ticker);
CREATE INDEX idx_campaign_status    ON campaign(status);
