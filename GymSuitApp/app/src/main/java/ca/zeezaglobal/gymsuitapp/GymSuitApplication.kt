package ca.zeezaglobal.gymsuitapp

import android.app.Application
import ca.zeezaglobal.gymsuitapp.di.AppComponent

class GymSuitApplication : Application() {

    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        appComponent = AppComponent.from(this)
    }
}
