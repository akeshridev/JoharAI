package com.akeshridev.johar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.akeshridev.johar.domain.crawl.ScheduleSourceCrawlUseCase

class MainViewModel(
    private val scheduleSourceCrawl: ScheduleSourceCrawlUseCase,
) : ViewModel() {
    fun crawlKnowledge() {
        scheduleSourceCrawl()
    }

    class Factory(
        private val scheduleSourceCrawl: ScheduleSourceCrawlUseCase,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MainViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(scheduleSourceCrawl) as T
        }
    }
}
