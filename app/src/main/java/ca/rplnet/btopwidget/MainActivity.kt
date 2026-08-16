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

        val styleContainer = findViewById<LinearLayout>(R.id.style_buttons)
        for (name in Prefs.BAR_STYLES.keys) {
            val (filled, empty) = Prefs.BAR_STYLES.getValue(name)
            val btn = Button(this).apply {
                text = "$name  (${filled.repeat(4)}${empty.repeat(4)})"
                setOnClickListener {
                    Prefs.setBarStyle(this@MainActivity, name)
                    refreshPreview()
                    pushWidgetUpdate()
                }
            }
            styleContainer.addView(btn)
        }

        refreshPreview()
    }

    override fun onResume() {
        super.onResume()
        refreshPreview()
    }

    private fun refreshPreview() {
        val stats = SystemStats.collect(this)
        val (filled, empty) = Prefs.getBarStyle(this)
        previewText.text = BtopRenderer.render(stats, filled, empty)
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
