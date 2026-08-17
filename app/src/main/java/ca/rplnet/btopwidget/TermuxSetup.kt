package ca.rplnet.btopwidget

// Commande a coller une seule fois dans Termux (interactif, pas via SSH) pour
// que la session se mette a logger vers le stockage partage. Idempotente
// (le grep evite de dupliquer le bloc si l'usager la roule 2 fois par erreur).
// N'importe qui qui installe l'app peut faire ca lui-meme, pas besoin qu'on
// le fasse a sa place.
object TermuxSetup {
    // le nom du dossier est fourni par l'usager (celui qu'il va choisir a
    // l'etape 2 via le selecteur de dossier partage) — pas fige en dur,
    // pour que ca marche peu importe comment il nomme son dossier
    fun command(folderName: String): String {
        val name = folderName.ifBlank { "btopdroid" }
        return """
            mkdir -p ~/storage/shared/$name && grep -q BTOPDROID_LOGGED ~/.bashrc 2>/dev/null || cat >> ~/.bashrc << 'EOF'

            # btopdroid: log la session interactive vers le stockage partage
            if [ -z "${'$'}BTOPDROID_LOGGED" ] && [ -t 1 ]; then
              export BTOPDROID_LOGGED=1
              exec script -qf ~/storage/shared/$name/session.log
            fi
            EOF
        """.trimIndent()
    }
}
