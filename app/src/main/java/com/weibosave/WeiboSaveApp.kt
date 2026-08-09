package com.weibosave

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.weibosave.data.UsageRepository
import com.weibosave.data.WeiboRepository

class WeiboSaveApp : Application() {
    override fun onCreate() {
        super.onCreate()
        UsageRepository.init(this)
        // Use the same OkHttpClient for Coil so TrafficInterceptor can measure view bytes.
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient { WeiboRepository.client }
                .build()
        )
    }
}
