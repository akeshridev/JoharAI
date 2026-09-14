package com.akeshridev.johar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akeshridev.johar.data.retrieval.LocalModelStore
import com.akeshridev.johar.data.spatial.RanchiSpatialPlace
import com.akeshridev.johar.di.JoharGraph
import com.akeshridev.johar.eval.OfflineRetrievalEvaluator
import com.akeshridev.johar.eval.RanchiCoverageEvaluator
import com.akeshridev.johar.map.RanchiMapCard

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
                val mapPreview by viewModel.mapPreview.collectAsStateWithLifecycle()
                DeveloperHarness(
                    mapPreview = mapPreview,
                    onPreviewMapClick = viewModel::previewRanchiMap,
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
    mapPreview: RanchiSpatialPlace?,
    onPreviewMapClick: (String) -> Unit,
    onTestOfflineClick: () -> Unit,
    onTestSpatialClick: () -> Unit,
    onTestAnswersClick: () -> Unit,
    onTestLlmClick: (String) -> Unit,
    onRunEvalClick: () -> Unit,
    onRunRanchiCoverageEvalClick: () -> Unit,
    onCrawlClick: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("Tagore Hill kahan hai?") }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ask Johar — Ranchi") },
                supportingText = { Text("Ranchi-only retrieval, spatial lookup and on-device Gemma") },
                minLines = 2,
            )
            Button(
                onClick = { onPreviewMapClick(query) },
                enabled = query.isNotBlank(),
            ) {
                Text("Preview Ranchi Map Card")
            }
            mapPreview?.let { place ->
                RanchiMapCard(destination = place)
            }
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
