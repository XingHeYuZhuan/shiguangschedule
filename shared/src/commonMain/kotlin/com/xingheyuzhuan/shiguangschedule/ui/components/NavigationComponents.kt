package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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

@Immutable
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
    navHideFractionProvider: () -> Float = { 0f },
    isTransparent: Boolean = false,
    navigationModifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val todayLabel = stringResource(Res.string.nav_today_schedule)
    val courseLabel = stringResource(Res.string.nav_course_schedule)
    val settingsLabel = stringResource(Res.string.nav_settings)

    val todaySelectedIcon = vectorResource(Res.drawable.view_agenda_filled_24px)
    val todayUnselectedIcon = vectorResource(Res.drawable.view_agenda_24px)
    val courseSelectedIcon = vectorResource(Res.drawable.view_week_filled_24px)
    val courseUnselectedIcon = vectorResource(Res.drawable.view_week_24px)
    val settingsSelectedIcon = vectorResource(Res.drawable.account_circle_filled_24px)
    val settingsUnselectedIcon = vectorResource(Res.drawable.account_circle_24px)

    val navItems = remember(
        todayLabel, courseLabel, settingsLabel,
        todaySelectedIcon, todayUnselectedIcon,
        courseSelectedIcon, courseUnselectedIcon,
        settingsSelectedIcon, settingsUnselectedIcon
    ) {
        listOf(
            NavItemData(
                label = todayLabel,
                destination = Destination.TodaySchedule,
                selectedIcon = todaySelectedIcon,
                unselectedIcon = todayUnselectedIcon
            ),
            NavItemData(
                label = courseLabel,
                destination = Destination.CourseSchedule,
                selectedIcon = courseSelectedIcon,
                unselectedIcon = courseUnselectedIcon
            ),
            NavItemData(
                label = settingsLabel,
                destination = Destination.Settings,
                selectedIcon = settingsSelectedIcon,
                unselectedIcon = settingsUnselectedIcon
            )
        )
    }

    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())

    Box(modifier = modifier.fillMaxSize()) {
        when (layoutType) {
            NavigationSuiteType.NavigationRail -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = showNavigation,
                        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)),
                        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300))
                    ) {
                        NavigationRail(
                            containerColor = if (isTransparent) Color.Transparent else NavigationRailDefaults.ContainerColor,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            navItems.forEach { item ->
                                key(item.destination) {
                                    val isSelected = currentDestination::class == item.destination::class
                                    NavigationRailItem(
                                        selected = isSelected,
                                        onClick = { if (!isSelected) onTabSelected(item.destination) },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        label = { Text(item.label, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        content(PaddingValues(0.dp))
                    }
                }
            }

            NavigationSuiteType.NavigationBar -> {
                val density = LocalDensity.current
                val navBarBottomInsetPx = with(density) {
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().toPx()
                }
                val navBarTotalHeightPx = with(density) { 88.dp.toPx() } + navBarBottomInsetPx

                Box(modifier = Modifier.fillMaxSize()) {
                    content(PaddingValues(0.dp))

                    AnimatedVisibility(
                        visible = showNavigation,
                        enter = slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn(tween(300)),
                        exit = slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut(tween(300)),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .then(navigationModifier)
                                .graphicsLayer {
                                    val fraction = navHideFractionProvider()
                                    translationY = navBarTotalHeightPx * fraction
                                    alpha = (1f - fraction).coerceIn(0f, 1f)
                                }
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isTransparent) Color.Transparent else NavigationBarDefaults.containerColor,
                                shadowElevation = 0.dp,
                                tonalElevation = 3.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    navItems.forEach { item ->
                                        key(item.destination) {
                                            val isSelected = currentDestination::class == item.destination::class

                                            NavigationTabItem(
                                                item = item,
                                                isSelected = isSelected,
                                                onSelect = { onTabSelected(item.destination) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                content(PaddingValues(0.dp))
            }
        }
    }
}

@Composable
private fun NavigationTabItem(
    item: NavItemData,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val pillBgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .drawBehind {
                drawRect(pillBgColor)
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isSelected) onSelect()
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.label,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    colorFilter = ColorFilter.tint(contentColor)
                }
        )

        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(tween(180)) + expandHorizontally(
                expandFrom = Alignment.Start,
                animationSpec = tween(220)
            ),
            exit = fadeOut(tween(120)) + shrinkHorizontally(
                shrinkTowards = Alignment.Start,
                animationSpec = tween(180)
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.label,
                    color = Color.Unspecified,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.graphicsLayer {
                        colorFilter = ColorFilter.tint(contentColor)
                    }
                )
            }
        }
    }
}