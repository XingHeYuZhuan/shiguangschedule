// com/xingheyuzhuan/shiguangschedule/ui/settings/additional/MoreOptionsScreen.kt
package com.xingheyuzhuan.shiguangschedule.ui.settings.additional

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xingheyuzhuan.shiguangschedule.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreOptionsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "更多选项") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = "查看开源许可证",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate(Screen.OpenSourceLicenses.route)
                    }
                    .padding(16.dp)
            )
            Text(
                text = "更新仓库",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // 导航到更新仓库页面
                        navController.navigate(Screen.UpdateRepo.route)
                    }
                    .padding(16.dp)
            )
        }
    }
}