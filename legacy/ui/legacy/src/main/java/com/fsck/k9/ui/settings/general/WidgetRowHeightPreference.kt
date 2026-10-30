package com.fsck.k9.ui.settings.general

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.fsck.k9.ui.R
import com.fsck.k9.widget.WidgetRowHeightSettings

/** Global row-height setting, stored outside GeneralSettingsDataStore for the widget module to read. */
class WidgetRowHeightPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {
    init {
        layoutResource = R.layout.preference_widget_row_height
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val valueView = holder.findViewById(R.id.widget_row_height_value) as TextView
        val seekBar = holder.findViewById(R.id.widget_row_height_seekbar) as SeekBar
        val height = WidgetRowHeightSettings.get(context)

        fun updateLabel(heightDp: Int) {
            valueView.text = context.getString(R.string.widget_row_height_value, heightDp)
        }

        seekBar.setOnSeekBarChangeListener(null)
        seekBar.max = WidgetRowHeightSettings.MAX_HEIGHT_DP - WidgetRowHeightSettings.MIN_HEIGHT_DP
        seekBar.progress = height - WidgetRowHeightSettings.MIN_HEIGHT_DP
        updateLabel(height)
        val resetButton = holder.findViewById(R.id.widget_row_height_reset) as Button
        resetButton.setOnClickListener {
            val defaultHeight = WidgetRowHeightSettings.DEFAULT_HEIGHT_DP
            if (callChangeListener(defaultHeight)) {
                WidgetRowHeightSettings.set(context, defaultHeight)
                seekBar.progress = defaultHeight - WidgetRowHeightSettings.MIN_HEIGHT_DP
                updateLabel(defaultHeight)
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) updateLabel(progress + WidgetRowHeightSettings.MIN_HEIGHT_DP)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val selected = seekBar.progress + WidgetRowHeightSettings.MIN_HEIGHT_DP
                if (callChangeListener(selected)) {
                    WidgetRowHeightSettings.set(context, selected)
                } else {
                    val original = WidgetRowHeightSettings.get(context)
                    seekBar.progress = original - WidgetRowHeightSettings.MIN_HEIGHT_DP
                    updateLabel(original)
                }
            }
        })
    }
}
