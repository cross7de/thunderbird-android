package net.thunderbird.feature.navigation.drawer.dropdown.ui.folder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
    treeFolder: DisplayTreeFolder? = null,
    parentPrefix: String? = "",
    indentationLevel: Int = 0,
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

    val expandable = treeFolder?.children?.isNotEmpty() == true

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                val step = guideStep.toPx()
                repeat(guideLevel) { index ->
                    val x = (index + 0.5f) * step
                    drawLine(
                        color = guideColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
            .padding(start = guideStep * guideLevel),
    ) {
        NavigationDrawerItem(
            label = {
                NavigationDrawerLabel(
                    label = mapFolderName(displayFolder, folderNameFormatter, parentPrefix),
                    expandable = expandable,
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
            modifier = Modifier.fillMaxWidth(),
            icon = { Icon(imageVector = mapFolderIcon(displayFolder)) },
        )
        if (expandable) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    // Account arrow: 16 dp outer + 16 dp inner + 12 dp icon center.
                    .padding(end = BoltTheme.spacings.double + BoltTheme.spacings.half)
                    .size(BoltTheme.sizes.iconAvatar)
                    .clip(CircleShape)
                    .clickable(onClick = onExpandClick),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedExpandIcon(isExpanded = isExpanded)
            }
        }
    }
}

@Composable
private fun NavigationDrawerLabel(
    label: String,
    badge: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    expandable: Boolean = false,
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
        badge()
        if (expandable) {
            // Reserve the arrow lane after the counters, not before them.
            Spacer(modifier = Modifier.width(BoltTheme.sizes.iconAvatar))
        }
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
