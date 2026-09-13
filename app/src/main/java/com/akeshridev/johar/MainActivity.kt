package com.akeshridev.johar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.akeshridev.johar.di.JoharGraph

class MainActivity : ComponentActivity() {
    private val graph by lazy { JoharGraph(applicationContext) }

    private val viewModel by lazy {
        ViewModelProvider(
            this,
            MainViewModel.Factory(
                scheduleSourceCrawl = graph.scheduleSourceCrawlUseCase,
                loadKnowledgePack = graph::loadKnowledgePack,
            ),
        )[MainViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DeveloperHarness(
                    onLoadPackClick = viewModel::loadKnowledgePack,
                    onCrawlClick = viewModel::crawlKnowledge,
                )
            }
        }
    }
}

@Composable
private fun DeveloperHarness(
    onLoadPackClick: () -> Unit,
    onCrawlClick: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = onLoadPackClick) {
                Text("Load Mega Knowledge Pack → Room")
            }
            Button(onClick = onCrawlClick) {
                Text("Crawl Live Jharkhand → Room + Logcat")
            }
        }
    }
}
