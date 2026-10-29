package net.thunderbird.feature.navigation.drawer.dropdown.ui.folder

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.k9mail.legacy.ui.folder.FolderNameFormatter
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextLabelLarge
import net.thunderbird.components.ui.bolt.organism.drawer.NavigationDrawerItem
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.feature.mail.folder.FolderType
import net.thunderbird.feature.navigation.drawer.dropdown.R
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayTreeFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolderType
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.AnimatedExpandIcon

private const val MAX_INDENTATION_LEVEL = 6

@Composable
internal fun FolderListItem(
    displayFolder: DisplayFolder,
    onClick: (DisplayFolder) -> Unit,
    showStarredCount: Boolean,
    folderNameFormatter: FolderNameFormatter,
    selectedFolderId: String?,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onExpandClick: () -> Unit = {},
    onExpandLongClick: () -> Unit = {},
    treeFolder: DisplayTreeFolder? = null,
    parentPrefix: String? = "",
    indentationLevel: Int = 0,
    guideSegments: List<FolderGuideSegment> = emptyList(),
) {
    var unreadCount = displayFolder.unreadMessageCount
    var starredCount = displayFolder.starredMessageCount

    if (treeFolder !== null && !isExpanded) {
        unreadCount = treeFolder.totalUnreadCount
        starredCount = treeFolder.totalStarredCount
    }

    val guideLevel = indentationLevel.coerceAtMost(MAX_INDENTATION_LEVEL)
    val guideStep = BoltTheme.spacings.half
    val guideColor = BoltTheme.colors.onSurfaceVariant.copy(alpha = 0.12f)

    NavigationDrawerItem(
        label = {
            NavigationDrawerLabel(
                label = mapFolderName(displayFolder, folderNameFormatter, parentPrefix),
                expandable = treeFolder?.children?.isNotEmpty() == true,
                isExpanded = isExpanded,
                onExpandClick = onExpandClick,
                onExpandLongClick = onExpandLongClick,
                badge = {
                    FolderListItemBadge(
                        unreadCount = unreadCount,
                        starredCount = starredCount,
                        showStarredCount = showStarredCount,
                    )
                },
            )
        },
        selected = selectedFolderId == displayFolder.folderId,
        onClick = {
            when (displayFolder) {
                is MailDisplayFolder if displayFolder.isPlaceholder -> onExpandClick()
                else -> onClick(displayFolder)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                val step = guideStep.toPx()
                guideSegments.forEach { segment ->
                    if (segment.level < MAX_INDENTATION_LEVEL) {
                        val x = (segment.level + 0.5f) * step
                        drawLine(
                            color = guideColor,
                            start = Offset(x, 0f),
                            end = Offset(x, if (segment.endsAtCenter) size.height / 2f else size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                }
                // Connect an expanded parent to its children from its vertical midpoint.
                if (
                    isExpanded &&
                    treeFolder?.children?.isNotEmpty() == true &&
                    guideLevel < MAX_INDENTATION_LEVEL
                ) {
                    val x = (guideLevel + 0.5f) * step
                    drawLine(
                        color = guideColor,
                        start = Offset(x, size.height / 2f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
            .padding(start = guideStep * guideLevel),
        icon = { Icon(imageVector = mapFolderIcon(displayFolder)) },
    )
}

@Composable
private fun NavigationDrawerLabel(
    label: String,
    badge: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    expandable: Boolean = false,
    isExpanded: Boolean = false,
    onExpandClick: () -> Unit = {},
    onExpandLongClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextLabelLarge(
            text = label,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
            modifier = Modifier.weight(1f),
        )
        if (expandable) {
            Box(
                modifier = Modifier
                    .size(BoltTheme.sizes.iconAvatar)
                    .padding(
                        start = BoltTheme.spacings.quarter,
                        end = BoltTheme.spacings.quarter,
                    )
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = onExpandClick,
                        onLongClick = onExpandLongClick,
                        onLongClickLabel = stringResource(
                            if (isExpanded) R.string.navigation_drawer_dropdown_collapse_subtree
                            else R.string.navigation_drawer_dropdown_expand_subtree,
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedExpandIcon(isExpanded = isExpanded)
            }
        }
        badge()
    }
}

@Composable
private fun mapFolderName(
    displayFolder: DisplayFolder,
    folderNameFormatter: FolderNameFormatter,
    parentPrefix: String? = "",
): String {
    return when (displayFolder) {
        is MailDisplayFolder ->
            folderNameFormatter
                .displayName(displayFolder.folder)
                .removePrefix("$parentPrefix${displayFolder.pathDelimiter}")

        is UnifiedDisplayFolder -> mapUnifiedFolderName(displayFolder)

        else -> throw IllegalArgumentException("Unknown display folder: $displayFolder")
    }
}

@Composable
private fun mapUnifiedFolderName(folder: UnifiedDisplayFolder): String {
    return when (folder.unifiedType) {
        UnifiedDisplayFolderType.INBOX -> stringResource(R.string.navigation_drawer_dropdown_unified_inbox_title)
    }
}

private fun mapFolderIcon(folder: DisplayFolder): ImageVector {
    return when (folder) {
        is MailDisplayFolder -> mapDisplayAccountFolderIcon(folder)
        is UnifiedDisplayFolder -> mapDisplayUnifiedFolderIcon(folder)
        else -> throw IllegalArgumentException("Unknown display folder type: $folder")
    }
}

private fun mapDisplayAccountFolderIcon(folder: MailDisplayFolder): ImageVector {
    if (folder.folder.type == FolderType.REGULAR && folder.isInTopGroup && !folder.isPlaceholder) {
        return Icons.Outlined.FavoriteFolder
    }

    return when (folder.folder.type) {
        FolderType.INBOX -> Icons.Outlined.Inbox
        FolderType.OUTBOX -> Icons.Outlined.Outbox
        FolderType.SENT -> Icons.Outlined.Send
        FolderType.TRASH -> Icons.Outlined.Delete
        FolderType.DRAFTS -> Icons.Outlined.Drafts
        FolderType.ARCHIVE -> Icons.Outlined.Archive
        FolderType.SPAM -> Icons.Outlined.Report
        FolderType.REGULAR -> Icons.Outlined.Folder
    }
}

private fun mapDisplayUnifiedFolderIcon(folder: UnifiedDisplayFolder): ImageVector {
    when (folder.unifiedType) {
        UnifiedDisplayFolderType.INBOX -> return Icons.Outlined.AllInbox
    }
}
