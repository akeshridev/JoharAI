package com.akeshridev.johar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.akeshridev.johar.data.retrieval.LocalModelStore
import com.akeshridev.johar.di.JoharGraph
import com.akeshridev.johar.eval.OfflineRetrievalEvaluator
import com.akeshridev.johar.eval.RanchiCoverageEvaluator

class MainActivity : ComponentActivity() {
    private val graph by lazy { JoharGraph(applicationContext) }

    private val viewModel by lazy {
        val evaluator = OfflineRetrievalEvaluator(
            context = applicationContext,
            retriever = graph.offlineKnowledgeRetriever,
        )
        val ranchiCoverageEvaluator = RanchiCoverageEvaluator(
            context = applicationContext,
            retriever = graph.offlineKnowledgeRetriever,
        )
        val localModelStore = LocalModelStore(applicationContext)
        ViewModelProvider(
            this,
            MainViewModel.Factory(
                scheduleSourceCrawl = graph.scheduleSourceCrawlUseCase,
                offlineKnowledgeRetriever = graph.offlineKnowledgeRetriever,
                ranchiSpatialEngine = graph.ranchiSpatialEngine,
                offlineRetrievalEvaluator = evaluator,
                ranchiCoverageEvaluator = ranchiCoverageEvaluator,
                localModelStore = localModelStore,
            ),
        )[MainViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DeveloperHarness(
                    onTestOfflineClick = viewModel::testOfflineRetrieval,
                    onTestSpatialClick = viewModel::testRanchiSpatial,
                    onTestAnswersClick = viewModel::testDeterministicAnswers,
                    onTestLlmClick = viewModel::testOnDeviceLlm,
                    onRunEvalClick = viewModel::runOfflineRetrievalEval,
                    onRunRanchiCoverageEvalClick = viewModel::runRanchiCoverageEval,
                    onCrawlClick = viewModel::crawlKnowledge,
                )
            }
        }
    }
}

@Composable
private fun DeveloperHarness(
    onTestOfflineClick: () -> Unit,
    onTestSpatialClick: () -> Unit,
    onTestAnswersClick: () -> Unit,
    onTestLlmClick: (String) -> Unit,
    onRunEvalClick: () -> Unit,
    onRunRanchiCoverageEvalClick: () -> Unit,
    onCrawlClick: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("Ranchi me parents ke saath kam walking wali jagah?") }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ask Johar — Ranchi") },
                supportingText = { Text("Runs local retrieval + Gemma; inspect JoharLLM in Logcat") },
                minLines = 2,
            )
            Button(
                onClick = { onTestLlmClick(query) },
                enabled = query.isNotBlank(),
            ) {
                Text("Run Query On-Device → Logcat")
            }
            Button(onClick = onTestOfflineClick) {
                Text("Test Ranchi RAG Retrieval → Logcat")
            }
            Button(onClick = onTestSpatialClick) {
                Text("Test Ranchi Spatial → Logcat")
            }
            Button(onClick = onTestAnswersClick) {
                Text("Test Deterministic Answers → Logcat")
            }
            Button(onClick = onRunEvalClick) {
                Text("Run Frozen Retrieval Eval → Logcat")
            }
            Button(onClick = onRunRanchiCoverageEvalClick) {
                Text("Run Ranchi Coverage Eval → Logcat")
            }
            Button(onClick = onCrawlClick) {
                Text("Crawl Ranchi Knowledge → Room + Logcat")
            }
        }
    }
}
