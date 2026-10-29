#!/usr/bin/env python3
"""Ergänze versuchsweise dezente vertikale Führungslinien in Prototyp 1.

Standard: Diff nur anzeigen. --apply: nach Quellprüfung genau eine Datei ändern.
"""
import argparse
import difflib
from pathlib import Path

RELATIVE_PATH = Path(
    "feature/navigation/drawer/dropdown/src/main/kotlin/"
    "net/thunderbird/feature/navigation/drawer/dropdown/ui/folder/FolderListItem.kt"
)


def replace_exact(source: str, old: str, new: str) -> str:
    hits = source.count(old)
    if hits != 1:
        raise ValueError(f"Abbruch: erwartete Prototyp-1-Quellstelle {hits}-mal gefunden (erwartet 1).")
    return source.replace(old, new, 1)


def create_revision(source: str) -> str:
    for old, new in [
        ("import androidx.compose.ui.draw.clip\n",
         "import androidx.compose.ui.draw.clip\nimport androidx.compose.ui.draw.drawWithContent\n"),
        ("import androidx.compose.ui.graphics.vector.ImageVector\n",
         "import androidx.compose.ui.geometry.Offset\nimport androidx.compose.ui.graphics.vector.ImageVector\n"),
        ("import androidx.compose.ui.text.style.TextOverflow\n",
         "import androidx.compose.ui.text.style.TextOverflow\nimport androidx.compose.ui.unit.dp\n"),
    ]:
        source = replace_exact(source, old, new)

    old_block = '''    NavigationDrawerItem(
        label = {
'''
    new_block = '''    val guideLevel = indentationLevel.coerceAtMost(MAX_INDENTATION_LEVEL)
    val guideStep = BoltTheme.spacings.half
    val guideColor = BoltTheme.colors.onSurfaceVariant.copy(alpha = 0.12f)

    NavigationDrawerItem(
        label = {
'''
    source = replace_exact(source, old_block, new_block)

    old_modifier = '''        modifier = modifier
            .fillMaxWidth()
            .padding(start = BoltTheme.spacings.half * indentationLevel.coerceAtMost(MAX_INDENTATION_LEVEL)),
'''
    new_modifier = '''        modifier = modifier
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
'''
    return replace_exact(source, old_modifier, new_modifier)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Testweise vertikale Einrückungs-Hilfslinien für Thunderbird-Android Prototyp 1."
    )
    parser.add_argument("repo", type=Path, help="Pfad zum lokalen Thunderbird-Android-Repository")
    parser.add_argument("--apply", action="store_true", help="Änderungen nach Prüfung schreiben")
    args = parser.parse_args()
    target = args.repo / RELATIVE_PATH
    if not target.is_file():
        parser.error(f"Nicht gefunden: {target}")
    old = target.read_text(encoding="utf-8")
    try:
        new = create_revision(old)
    except ValueError as error:
        parser.error(str(error))
    if args.apply:
        target.write_text(new, encoding="utf-8")
        print(f"Geändert: {target}")
        print("Nächster Schritt: git diff --check; Modultest und Debug-Build ausführen.")
    else:
        diff = difflib.unified_diff(
            old.splitlines(keepends=True),
            new.splitlines(keepends=True),
            fromfile=f"a/{RELATIVE_PATH}",
            tofile=f"b/{RELATIVE_PATH}",
        )
        print("".join(diff), end="")
        print("\nNur Vorschau. Mit --apply wird die Änderung geschrieben.")


if __name__ == "__main__":
    main()
