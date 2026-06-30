package com.tradelog.price;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tradelog.price")
public class PriceProperties {
    private int cacheTtlMinutes = 5;
    private int timeoutSeconds  = 3;

    public int getCacheTtlMinutes() { return cacheTtlMinutes; }
    public void setCacheTtlMinutes(int v) { this.cacheTtlMinutes = v; }
    public int getTimeoutSeconds()        { return timeoutSeconds; }
    public void setTimeoutSeconds(int v)  { this.timeoutSeconds = v; }
}
