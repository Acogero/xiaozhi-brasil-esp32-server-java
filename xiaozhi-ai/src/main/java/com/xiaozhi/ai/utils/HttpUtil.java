package com.xiaozhi.ai.utils;

import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

/**
 * Classe utilitária HTTP
 * Usada para criar instâncias de OkHttpClient
 */
public class HttpUtil {
    /**
     * Instância de OkHttpClient
     */
    public static final OkHttpClient client;

    static{
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
}
