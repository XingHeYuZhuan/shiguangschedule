package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItemColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.Destination
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.account_circle_24px
import shiguangschedule.shared.generated.resources.account_circle_filled_24px
import shiguangschedule.shared.generated.resources.nav_course_schedule
import shiguangschedule.shared.generated.resources.nav_settings
import shiguangschedule.shared.generated.resources.nav_today_schedule
import shiguangschedule.shared.generated.resources.view_agenda_24px
import shiguangschedule.shared.generated.resources.view_agenda_filled_24px
import shiguangschedule.shared.generated.resources.view_week_24px
import shiguangschedule.shared.generated.resources.view_week_filled_24px

private data class NavItemData(
    val label: String,
    val destination: Destination,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * 自适应导航栏组件
 */
@Composable
fun AdaptiveNavigationScaffold(
    currentDestination: Destination,
    onTabSelected: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    showNavigation: Boolean = true,
    isTransparent: Boolean = false,
    contentColor: Color? = null,
    navigationModifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val navItems = listOf(
        NavItemData(
            label = stringResource(Res.string.nav_today_schedule),
            destination = Destination.TodaySchedule,
            selectedIcon = vectorResource(Res.drawable.view_agenda_filled_24px),
            unselectedIcon = vectorResource(Res.drawable.view_agenda_24px)
        ),
        NavItemData(
            label = stringResource(Res.string.nav_course_schedule),
            destination = Destination.CourseSchedule,
            selectedIcon = vectorResource(Res.drawable.view_week_filled_24px),
            unselectedIcon = vectorResource(Res.drawable.view_week_24px)
        ),
        NavItemData(
            label = stringResource(Res.string.nav_settings),
            destination = Destination.Settings,
            selectedIcon = vectorResource(Res.drawable.account_circle_filled_24px),
            unselectedIcon = vectorResource(Res.drawable.account_circle_24px)
        )
    )

    val iconSize = 24.dp
    val textSize = 12.sp

    val finalContentColor = contentColor ?: MaterialTheme.colorScheme.onSurface
    val finalSubTextColor = finalContentColor.copy(alpha = 0.7f)

    val itemColors: NavigationSuiteItemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            indicatorColor = if (isTransparent) Color.Transparent else MaterialTheme.colorScheme.secondaryContainer,
            selectedIconColor = if (contentColor != null) finalContentColor else MaterialTheme.colorScheme.onSecondaryContainer,
            selectedTextColor = if (contentColor != null) finalContentColor else MaterialTheme.colorScheme.onSurface,
            unselectedIconColor = if (contentColor != null) finalSubTextColor else MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = if (contentColor != null) finalSubTextColor else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            indicatorColor = if (isTransparent) Color.Transparent else MaterialTheme.colorScheme.secondaryContainer,
            selectedIconColor = if (contentColor != null) finalContentColor else MaterialTheme.colorScheme.onSecondaryContainer,
            selectedTextColor = if (contentColor != null) finalContentColor else MaterialTheme.colorScheme.onSurface,
            unselectedIconColor = if (contentColor != null) finalSubTextColor else MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = if (contentColor != null) finalSubTextColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    )

    val layoutType = if (!showNavigation) {
        NavigationSuiteType.None
    } else {
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (layoutType) {
            NavigationSuiteType.NavigationRail -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = if (isTransparent) Color.Transparent else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        navItems.forEach { item ->
                            val isSelected = currentDestination::class == item.destination::class
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { if (!isSelected) onTabSelected(item.destination) },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                        modifier = Modifier.size(iconSize)
                                    )
                                },
                                label = { Text(item.label, fontSize = textSize) },
                                colors = itemColors.navigationRailItemColors
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        content(PaddingValues(0.dp))
                    }
                }
            }
            NavigationSuiteType.NavigationBar -> {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    bottomBar = {
                        NavigationBar(
                            containerColor = if (isTransparent) Color.Transparent else MaterialTheme.colorScheme.surface,
                            modifier = navigationModifier
                        ) {
                            navItems.forEach { item ->
                                val isSelected = currentDestination::class == item.destination::class
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { if (!isSelected) onTabSelected(item.destination) },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label,
                                            modifier = Modifier.size(iconSize)
                                        )
                                    },
                                    label = { Text(item.label, fontSize = textSize) },
                                    colors = itemColors.navigationBarItemColors
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    content(innerPadding)
                }
            }
            else -> {
                content(PaddingValues(0.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdaptiveNavigationScaffoldPreview() {
    MaterialTheme {
        AdaptiveNavigationScaffold(
            currentDestination = Destination.CourseSchedule,
            onTabSelected = {}
        ) { _ ->
            // Preview Content
        }
    }
}