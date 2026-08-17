package ca.rplnet.btopwidget

import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val pickFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@registerForActivityResult
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        TermuxLog.setTreeUri(this, uri)
        pushWidgetUpdate()
    }

    private lateinit var previewText: TextView
    private lateinit var usernameInput: EditText
    private lateinit var hostnameInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewText = findViewById(R.id.preview_text)
        usernameInput = findViewById(R.id.input_username)
        hostnameInput = findViewById(R.id.input_hostname)

        usernameInput.setText(Prefs.getUsername(this))
        hostnameInput.setText(Prefs.getHostname(this))

        val folderInput = findViewById<EditText>(R.id.input_folder_name)
        val step2Label = findViewById<TextView>(R.id.label_step2)
        folderInput.setText(Prefs.getTermuxFolderName(this))
        fun currentFolderName() = folderInput.text.toString().ifBlank { "btopdroid" }
        fun updateStep2Label() {
            step2Label.text = "Étape 2 — choisis le dossier \"${currentFolderName()}\" dans le sélecteur:"
        }
        updateStep2Label()
        folderInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                Prefs.setTermuxFolderName(this@MainActivity, currentFolderName())
                updateStep2Label()
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        findViewById<Button>(R.id.btn_copy_termux_cmd).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("btopdroid setup", TermuxSetup.command(currentFolderName())))
            Toast.makeText(this, "Commande copiée — colle-la dans Termux", Toast.LENGTH_LONG).show()
        }

        findViewById<Button>(R.id.btn_pick_termux_folder).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            pickFolderLauncher.launch(intent)
        }

        val liveBtn = findViewById<Button>(R.id.btn_toggle_live)
        fun updateLiveBtnText() {
            liveBtn.text = if (Prefs.isLiveUpdateEnabled(this)) "Désactiver" else "Activer"
        }
        updateLiveBtnText()
        liveBtn.setOnClickListener {
            if (Prefs.isLiveUpdateEnabled(this)) {
                LiveUpdateService.stop(this)
            } else {
                LiveUpdateService.start(this)
            }
            updateLiveBtnText()
        }

        findViewById<Button>(R.id.btn_save_identity).setOnClickListener {
            val user = usernameInput.text.toString().ifBlank { "user" }
            val host = hostnameInput.text.toString().ifBlank { "android" }
            Prefs.setIdentity(this, user, host)
            refreshPreview()
            pushWidgetUpdate()
        }

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
        val text = BtopRenderer.render(this, stats, Prefs.getUsername(this), Prefs.getHostname(this), Prefs.getFgColor(this))
        previewText.text = text
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
