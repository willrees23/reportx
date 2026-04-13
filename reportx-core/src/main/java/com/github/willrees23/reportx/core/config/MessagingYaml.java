package com.github.willrees23.reportx.core.config;

public record MessagingYaml(
        String transport,
        Redis redis
) {

    public record Redis(
            String host,
            int port,
            String password,
            String channelPrefix
    ) {
    }
}
