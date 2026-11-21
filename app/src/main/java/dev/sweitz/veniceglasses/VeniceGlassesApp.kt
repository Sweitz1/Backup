package dev.sweitz.veniceglasses

import android.app.Application
import com.ffalcon.mercury.android.sdk.MercurySDK
import dev.sweitz.veniceglasses.data.CardDownloader
import dev.sweitz.veniceglasses.data.CharacterStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import dev.sweitz.veniceglasses.data.RequestLog
import dev.sweitz.veniceglasses.data.Settings
import dev.sweitz.veniceglasses.data.VeniceClient
import java.io.File

class VeniceGlassesApp : Application() {
    lateinit var settings: Settings
        private set
    lateinit var requestLog: RequestLog
        private set
    lateinit var venice: VeniceClient
        private set
    lateinit var characters: CharacterStore
        private set
    lateinit var cardDownloader: CardDownloader
        private set

    /** For work that outlives a screen, e.g. a card download started over adb. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        MercurySDK.init(this) // RayNeo SDK: required before any mirror activity starts

        settings = Settings(this)
        requestLog = RequestLog(File(filesDir, "requests-24h.csv"))
        venice = VeniceClient(settings, requestLog)
        characters = CharacterStore(this)
        cardDownloader = CardDownloader(characters)
    }
}

val android.content.Context.app: VeniceGlassesApp get() = applicationContext as VeniceGlassesApp
