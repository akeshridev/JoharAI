package com.akeshridev.johar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akeshridev.johar.data.pack.KnowledgePackLoadResult
import com.akeshridev.johar.domain.crawl.ScheduleSourceCrawlUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(
    private val scheduleSourceCrawl: ScheduleSourceCrawlUseCase,
    private val loadKnowledgePack: () -> KnowledgePackLoadResult,
) : ViewModel() {

    fun loadKnowledgePack() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching(loadKnowledgePack)
                .onSuccess { result ->
                    Log.i(
                        TAG,
                        "loaded version=${result.version} entities=${result.entities} " +
                            "facts=${result.facts} relationships=${result.relationships}",
                    )
                }
                .onFailure { error -> Log.e(TAG, "load_failed", error) }
        }
    }

    fun crawlKnowledge() {
        scheduleSourceCrawl()
    }

    class Factory(
        private val scheduleSourceCrawl: ScheduleSourceCrawlUseCase,
        private val loadKnowledgePack: () -> KnowledgePackLoadResult,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MainViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(scheduleSourceCrawl, loadKnowledgePack) as T
        }
    }

    companion object {
        private const val TAG = "JoharPack"
    }
}
