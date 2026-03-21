package com.scrolllight.bible.ui.plans

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scrolllight.bible.data.model.PlanCategory
import com.scrolllight.bible.data.model.ReadingPlan
import com.scrolllight.bible.ui.components.PlanCard
import com.scrolllight.bible.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(vm: PlansViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("我的计划", "所有计划", "已完成")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("读经计划", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> MyPlansContent()
                1 -> AllPlansContent(plans = state.allPlans, onStart = { vm.startPlan(it) })
                2 -> CompletedPlansContent()
            }
        }
    }
}

@Composable
private fun MyPlansContent() {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("还没有进行中的计划，去「所有计划」挑选一个吧！",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CompletedPlansContent() {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("还没有完成的计划。", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AllPlansContent(plans: List<ReadingPlan>, onStart: (ReadingPlan) -> Unit) {
    val grouped = plans.groupBy { it.category }
    val order = listOf(PlanCategory.THEME, PlanCategory.WHOLE_BIBLE, PlanCategory.NEW_TESTAMENT, PlanCategory.OLD_TESTAMENT)

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        order.forEach { category ->
            grouped[category]?.let { categoryPlans ->
                item {
                    SectionHeader(
                        title = category.displayName,
                        action = "查看全部",
                        onAction = {}
                    )
                    Spacer(Modifier.height(8.dp))
                }
                items(categoryPlans) { plan ->
                    PlanCard(
                        title = plan.title,
                        days = plan.days,
                        onClick = { onStart(plan) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }

        // Custom plan
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("自定义计划", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("创建属于你的个人读经计划", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {}, shape = RoundedCornerShape(10.dp)) {
                        Text("创建")
                    }
                }
            }
        }
    }
}
