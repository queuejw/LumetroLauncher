package ru.queuejw.lumetro.components.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.queuejw.lumetro.components.core.ColorManager
import ru.queuejw.lumetro.components.core.db.tile.TileDatabase
import ru.queuejw.lumetro.components.core.icons.IconLoader
import ru.queuejw.lumetro.model.App
import ru.queuejw.lumetro.model.TileEntity

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var _appsList: MutableLiveData<List<App>>? = null
    private val appsList get() = _appsList!!

    private var _colorManager: ColorManager? = null
    private val mColorManager get() = _colorManager!!
    private var tilesList: MutableLiveData<MutableList<TileEntity>>? = null

    private var db: TileDatabase? = null
    private var iconLoader: IconLoader? = null
    var appsScrollPosition: Int? = null

    private fun createDatabase(context: Context): TileDatabase {
        return TileDatabase.getTileData(context)
    }

    init {
        db = createDatabase(application.applicationContext)
        _colorManager = ColorManager()
        _appsList = MutableLiveData<List<App>>(ArrayList())
        tilesList = MutableLiveData<MutableList<TileEntity>>()
    }

    fun createIconLoader(boolean: Boolean = false, string: String? = null) {
        iconLoader = IconLoader(boolean, string)
    }

    fun getIconLoader(): IconLoader? {
        return iconLoader
    }

    fun cleanUp() {
        _colorManager?.clearColors()
        iconLoader?.resetIconLoader()
        iconLoader = null
        db = null
    }

    private fun destroyViewModelComponents() {
        _colorManager?.clearColors()
        iconLoader?.resetIconLoader(true)
        iconLoader = null
        db = null
        _appsList = null
        tilesList?.value?.clear()
        tilesList = null
        _colorManager = null
        appsScrollPosition = null
        viewModelScope.cancel()
    }

    override fun onCleared() {
        destroyViewModelComponents()
        super.onCleared()
    }

    fun updateAppsList(newValue: List<App>?) {
        if (newValue != null) {
            appsList.postValue(newValue)
        }
    }

    fun getLiveAppsList(): MutableLiveData<List<App>> {
        return appsList
    }

    fun updateTilesList(newValue: MutableList<TileEntity>?) {
        if (newValue != null) {
            tilesList?.postValue(newValue)
        }
    }

    fun getTiles(): MutableLiveData<MutableList<TileEntity>>? {
        return tilesList
    }

    fun getColorManager(): ColorManager {
        return mColorManager
    }

    fun getDatabase(): TileDatabase {
        db?.let {
            return it
        }
        db = createDatabase(application.applicationContext)
        return db!!
    }

    fun updateTilePositions(list: MutableList<TileEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            val newData = ArrayList<TileEntity>()
            for (i in 0 until list.size) {
                list[i].tilePosition = i
                newData.add(list[i])
            }
            updateTilesList(newData)
            getDatabase().getTilesDao().updateAllTiles(newData)
        }
    }
}