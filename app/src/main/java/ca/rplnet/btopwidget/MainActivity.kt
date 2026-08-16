package ca.rplnet.btopwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var previewText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewText = findViewById(R.id.preview_text)

        val colorContainer = findViewById<LinearLayout>(R.id.color_buttons)
        for (name in Prefs.COLOR_PRESETS.keys) {
            val btn = Button(this).apply {
                text = name
                setOnClickListener {
                    Prefs.setColorPreset(this@MainActivity, name)
                    refreshPreview()
                    pushWidgetUpdate()
                }
            }
            colorContainer.addView(btn)
        }

        refreshPreview()
    }

    override fun onResume() {
        super.onResume()
        refreshPreview()
    }

    private fun refreshPreview() {
        val stats = SystemStats.collect(this)
        previewText.text = BtopRenderer.renderHeader(stats) + "\n\n" + BtopRenderer.renderFooter(stats) +
            "\n\n(les graphs ram/cpu/bat s'affichent directement sur le widget)"
        previewText.setTextColor(Prefs.getFgColor(this))
        previewText.setBackgroundColor(Prefs.getBgColor(this))
    }

    private fun pushWidgetUpdate() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(this, BtopWidgetProvider::class.java)
        )
        for (id in widgetIds) {
            BtopWidgetProvider.updateWidget(this, appWidgetManager, id)
        }
    }
}
