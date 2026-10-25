#!/usr/bin/env bash
# Builds the Debian package amade-app-handbook: the handbook for reading without a network
# (mkdocs-offline.yml) in /usr/share/doc/amade-app-handbook/html and a desktop entry of category
# Documentation, so the Help drawer of the amadeOS Front Panel lists it and opens it in the web
# browser (xdg-open, Firefox in amadeOS; the pages need JavaScript).
#
# Usage: docs/handbook/package-deb.sh VERSION OUTPUT_DIR
#
# Like the published handbook it includes code from the application repositories (amade-pad, ...),
# which must be checked out next to amade-commons. The excerpts are taken at the commits pinned in
# .github/workflows/handbook.yml, as CI does, not from the checkouts as they are: an application's
# main branch may already have moved or removed code a chapter shows. Missing commits are fetched.
# MkDocs comes from requirements.txt, installed into .venv here if no complete mkdocs is on the PATH. Prints the
# path of the package as its last line.
set -euo pipefail

version="${1:?version, e.g. 0.1.0}"
out_dir="${2:?output directory}"
handbook_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
commons_dir="$(cd "$handbook_dir/../.." && pwd)"
parent_dir="$(dirname "$commons_dir")"
workflow="$commons_dir/.github/workflows/handbook.yml"
package=amade-app-handbook

mkdocs=mkdocs
# The mkdocs on the PATH only counts when python3 also has the theme and extensions of mkdocs.yml: the
# plain Debian package mkdocs comes without them.
if ! command -v mkdocs >/dev/null 2>&1 || ! python3 -c 'import material, pymdownx' 2>/dev/null; then
    if [ ! -x "$handbook_dir/.venv/bin/mkdocs" ]; then
        python3 -m venv "$handbook_dir/.venv"
        "$handbook_dir/.venv/bin/pip" install --quiet -r "$handbook_dir/requirements.txt"
    fi
    mkdocs="$handbook_dir/.venv/bin/mkdocs"
fi

staging="$(mktemp -d)"
sources="$(mktemp -d)"
trap 'rm -rf "$staging" "$sources"' EXIT
doc_dir="$staging/usr/share/doc/$package"
mkdir -p "$doc_dir" "$staging/usr/share/applications" "$staging/DEBIAN" "$out_dir"

# The sources the book is built from: amade-commons as it is checked out here, and every repository the
# workflow checks out at the commit it pins ("repository: datazuul/<name>" followed by
# "ref: ${{ env.<NAME>_COMMIT }}", the commit in the env block at the top).
mkdir -p "$sources/amade-commons"
tar -C "$commons_dir" --exclude=.venv --exclude=site -cf - docs src | tar -C "$sources/amade-commons" -xf -
while read -r repo commit; do
    if [ ! -d "$parent_dir/$repo/.git" ]; then
        echo "$repo is not checked out next to amade-commons ($parent_dir/$repo)" >&2
        exit 1
    fi
    if ! git -C "$parent_dir/$repo" cat-file -e "$commit^{commit}" 2>/dev/null; then
        git -C "$parent_dir/$repo" fetch --quiet origin "$commit"
    fi
    mkdir -p "$sources/$repo"
    git -C "$parent_dir/$repo" archive "$commit" | tar -C "$sources/$repo" -xf -
done < <(awk '
    /^  [A-Z_]+_COMMIT:/ { name = $1; sub(":", "", name); commit[name] = $2 }
    /repository: datazuul\// { repo = $2; sub("datazuul/", "", repo) }
    /ref: \$\{\{ env\.[A-Z_]+_COMMIT \}\}/ && repo != "" {
        name = $0; sub(/.*env\./, "", name); sub(/ .*/, "", name)
        print repo, commit[name]; repo = ""
    }' "$workflow")

(cd "$sources/amade-commons/docs/handbook" && "$mkdocs" build --quiet -f mkdocs-offline.yml --site-dir "$doc_dir/html")
cp "$handbook_dir/../../LICENSE" "$doc_dir/copyright"

cat > "$staging/usr/share/applications/$package.desktop" <<DESKTOP
[Desktop Entry]
Type=Application
Name=Application Developer's Handbook
Name[de]=Handbuch für Anwendungsentwickler
Comment=Learn to write desktop applications in Java by building the amadeOS applications
Comment[de]=Desktop-Anwendungen in Java schreiben lernen, an den Anwendungen von amadeOS
Exec=xdg-open /usr/share/doc/$package/html/index.html
TryExec=xdg-open
Icon=help-browser
Categories=Documentation;Development;
DESKTOP

cat > "$staging/DEBIAN/control" <<CONTROL
Package: $package
Version: $version
Section: doc
Priority: optional
Architecture: all
Installed-Size: $(du -sk "$staging/usr" | cut -f1)
Recommends: firefox-esr | www-browser, xdg-utils
Maintainer: amadeOS Project <developers@datazuul.com>
Description: amadeOS Application Developer's Handbook
 Learn to write desktop applications in Java by building the amadeOS
 applications and their shared library, amade-commons, one application at
 a time: from the text editor amade-pad to mail, office, graphics and
 media programs. The pages open in the web browser and need no network.
CONTROL

deb="$out_dir/${package}_${version}_all.deb"
dpkg-deb --root-owner-group --build "$staging" "$deb" >/dev/null
echo "$deb"
