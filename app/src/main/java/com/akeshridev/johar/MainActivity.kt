package com.akeshridev.johar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.akeshridev.johar.di.JoharGraph

class MainActivity : ComponentActivity() {
    private val graph by lazy { JoharGraph(applicationContext) }

    private val viewModel by lazy {
        ViewModelProvider(
            this,
            MainViewModel.Factory(graph.scheduleSourceCrawlUseCase),
        )[MainViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CrawlTrigger(onCrawlClick = viewModel::crawlDassam)
            }
        }
    }
}

@Composable
private fun CrawlTrigger(onCrawlClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Button(onClick = onCrawlClick) {
                Text("Crawl Dassam → Room + Logcat")
            }
        }
    }
}
