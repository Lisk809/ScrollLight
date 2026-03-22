package com.scrolllight.bible

import android.app.Application
import android.util.Log
import com.scrolllight.bible.data.library.LibraryRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.*
import javax.inject.Inject

@HiltAndroidApp
class ScrollLightApp : Application() {

    @Inject lateinit var libraryRepo: LibraryRepository

    override fun onCreate() {
        super.onCreate()
        installBuiltInBibles()
    }

    /**
     * 在后台线程安装 assets/ 里的所有 .slbook 文件。
     * 使用 GlobalScope 保证安装任务不受 Activity 生命周期影响。
     * 已安装的版本会自动跳过，所以多次启动不会重复解析。
     */
    private fun installBuiltInBibles() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val installed = libraryRepo.installBuiltInAssets()
                if (installed.isNotEmpty()) {
                    Log.i("ScrollLight", "Built-in Bibles installed: $installed")
                }
            } catch (e: Exception) {
                Log.e("ScrollLight", "Failed to install built-in assets: ${e.message}")
            }
        }
    }
}
