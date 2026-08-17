package ca.rplnet.btopwidget

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

// Lit le log de session Termux via le stockage partage (Storage Access
// Framework) — Termux et btopdroid sont deux apps sandboxees, aucune ne
// peut lire les fichiers privees de l'autre. Termux ecrit son log dans
// ~/storage/shared/btopdroid/session.log (voir setup cote Termux dans
// .bashrc), et l'usager doit accorder l'acces a ce dossier une fois via
// le selecteur systeme (ACTION_OPEN_DOCUMENT_TREE).
object TermuxLog {
    private const val KEY_TREE_URI = "termux_tree_uri"
    private const val LOG_FILENAME = "session.log"
    private const val MAX_LINES = 6

    fun setTreeUri(context: Context, uri: Uri) {
        context.getSharedPreferences("btopdroid_prefs", Context.MODE_PRIVATE)
            .edit().putString(KEY_TREE_URI, uri.toString()).apply()
    }

    fun getTreeUri(context: Context): Uri? {
        val raw = context.getSharedPreferences("btopdroid_prefs", Context.MODE_PRIVATE)
            .getString(KEY_TREE_URI, null) ?: return null
        return Uri.parse(raw)
    }

    fun isConfigured(context: Context): Boolean = getTreeUri(context) != null

    // retourne les dernieres lignes du log, ou null si pas configure /
    // fichier introuvable (l'appelant decide quoi afficher dans ce cas)
    fun readTail(context: Context): List<String>? {
        val treeUri = getTreeUri(context) ?: return null
        return try {
            val dir = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            val file = dir.findFile(LOG_FILENAME) ?: return null
            val resolver = context.contentResolver
            val text = resolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() }
                ?: return null
            val lines = text.lines().filter { it.isNotBlank() }
            lines.takeLast(MAX_LINES)
        } catch (e: Exception) {
            null
        }
    }
}
