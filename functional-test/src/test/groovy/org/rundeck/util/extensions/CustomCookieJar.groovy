package org.rundeck.util.extensions

import okhttp3.*
import org.jetbrains.annotations.NotNull

class CustomCookieJar implements CookieJar {

    List<Cookie> cookieStore = []

    @Override
    List<Cookie> loadForRequest(@NotNull HttpUrl httpUrl) {
        return cookieStore
    }

    @Override
    void saveFromResponse(@NotNull HttpUrl httpUrl, @NotNull List<Cookie> list) {
        cookieStore.addAll(list)
    }
}
