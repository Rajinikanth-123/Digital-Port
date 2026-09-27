package com.portshipping.config;
import java.time.Instant;
import java.util.Map;
public record ErrorResponse(Instant timestamp,int status,String error,String message,String path,
                            Map<String,String> validation) {
    public ErrorResponse(Instant timestamp,int status,String error,String message,String path) {
        this(timestamp,status,error,message,path,Map.of());
    }
}
