package com.scrolllight.bible.ui.plans

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scrolllight.bible.data.model.PlanCategory
import com.scrolllight.bible.data.model.ReadingPlan
import com.scrolllight.bible.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(vm: PlansViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("我的计划", "所有计划", "已完成")

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("读经计划", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = Color.Transparent,
                contentColor     = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(selected = selectedTab == idx, onClick = { selectedTab = idx },
                        text = { Text(title, style = MaterialTheme.typography.labelLarge) })
                }
            }
            when (selectedTab) {
                0 -> EmptyTabContent("还没有进行中的计划，\n去「所有计划」挑一个吧！")
                1 -> AllPlansContent(state.allPlans) { vm.startPlan(it) }
                2 -> EmptyTabContent("还没有完成的计划。")
            }
        }
    }
}

@Composable
private fun EmptyTabContent(msg: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
        AuroraCard(modifier = Modifier.fillMaxWidth()) {
            Text(msg, modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AllPlansContent(plans: List<ReadingPlan>, onStart: (ReadingPlan) -> Unit) {
    val grouped = plans.groupBy { it.category }
    val order   = listOf(PlanCategory.THEME, PlanCategory.WHOLE_BIBLE, PlanCategory.NEW_TESTAMENT, PlanCategory.OLD_TESTAMENT)

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        order.forEach { category ->
            grouped[category]?.let { catPlans ->
                item {
                    AuroraSectionHeader(category.displayName, "查看全部", {})
                    Spacer(Modifier.height(8.dp))
                }
                items(catPlans) { plan ->
                    AuroraPlanCard(title = plan.title, days = plan.days, onClick = { onStart(plan) },
                        modifier = Modifier.fillMaxWidth())
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
        item {
            AuroraCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("自定义计划", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("创建专属读经计划", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = {}, shape = RoundedCornerShape(12.dp)) { Text("创建") }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}
