package com.fsck.k9.ui.choosefolder

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.ui.R
import com.google.android.material.textview.MaterialTextView
import com.mikepenz.fastadapter.items.AbstractItem

internal class FolderListItem(
    val databaseId: Long?,
    private val folderIconResource: Int,
    val displayName: String,
    private val label: String,
    val path: String,
    private val depth: Int,
    val hasChildren: Boolean,
    private val showExpansionButton: Boolean,
    private val isExpanded: Boolean,
    val canSelectDestination: Boolean,
    private val onToggleExpansion: (String) -> Unit,
    private val onSelectDestination: (FolderListItem) -> Unit,
) : AbstractItem<FolderListViewHolder>() {
    // Placeholder paths have no real folder ID and must never be returned as move/copy targets.
    override var identifier = databaseId ?: -(path.hashCode().toLong() and 0xffffffffL) - 1L

    override val layoutRes = R.layout.folder_list_item
    override val type: Int = 1

    override fun getViewHolder(v: View) = FolderListViewHolder(v)

    override fun bindView(holder: FolderListViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.icon.setImageResource(folderIconResource)
        holder.name.text = label
        holder.name.alpha = if (canSelectDestination) 1f else 0.6f
        holder.icon.alpha = if (canSelectDestination) 1f else 0.6f

        val density = holder.itemView.resources.displayMetrics.density
        val params = holder.icon.layoutParams as LinearLayout.LayoutParams
        params.marginStart = ((16 + depth.coerceAtMost(6) * 12) * density).toInt()
        holder.icon.layoutParams = params

        holder.selectDestination.visibility = if (canSelectDestination) View.VISIBLE else View.GONE
        holder.selectDestination.setOnClickListener(
            if (canSelectDestination) View.OnClickListener { onSelectDestination(this) } else null,
        )

        holder.expand.visibility = if (showExpansionButton) View.VISIBLE else View.GONE
        if (showExpansionButton) {
            holder.expand.rotation = if (isExpanded) 0f else -90f
            holder.expand.contentDescription = holder.itemView.context.getString(
                if (isExpanded) R.string.choose_folder_collapse else R.string.choose_folder_expand,
            )
            holder.expand.setOnClickListener { onToggleExpansion(path) }
        } else {
            holder.expand.setOnClickListener(null)
        }
    }
}

internal class FolderListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val icon: ImageView = itemView.findViewById(R.id.folder_icon)
    val name: MaterialTextView = itemView.findViewById(R.id.folder_name)
    val expand: ImageButton = itemView.findViewById(R.id.folder_expand)
    val selectDestination: TextView = itemView.findViewById(R.id.folder_choose_destination)
}
