# amadeOS Application Developer's Handbook

The handbook teaches desktop programming in Java by building the amadeOS applications and their shared library, amade-commons, one application at a time. It is published at <https://datazuul.github.io/amade-commons/>.

## Writing

- One Markdown file per chapter in `chapters/`, numbered: `0x` getting started, `1x` part I, `2x` part II, `3x` part III, `4x` part IV (the amade-commons manual, by subsystem rather than by application). The order of the book is the `nav` in `mkdocs.yml`.
- Pictures go to `chapters/images/<app>/`.
- **Code is never copied into a chapter.** It is included from amade-commons or from the application's repository, from a section marked in the source:

  ```java
  // --8<-- [start:decode]
  public TextDocument decode(byte[] bytes) throws IOException { … }
  // --8<-- [end:decode]
  ```

  and in the chapter, inside a code block:

  ```text
  --8<-- "amade-pad/src/main/java/eu/amadeos/apps/pad/repository/TextCodec.java:decode"
  ```

  Paths start at the directory that holds the repositories, so the applications, and amadeos-system for Part VI, must be checked out next to `amade-commons`. The book then always shows code that compiles and is tested.
- The workflow `.github/workflows/handbook.yml` checks out each application and amadeos-system at a fixed commit (`PAD_COMMIT`, …, `AMADEOS_SYSTEM_COMMIT`). Raise it when a chapter is updated for a newer state of the application.
- When a chapter moves code into amade-commons, the same pull request adds the chapter and the moved code; the applications follow with pull requests that use the new commons commit.

## Building locally

```bash
cd docs/handbook
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
.venv/bin/mkdocs serve          # http://127.0.0.1:8000, reloads on every save
.venv/bin/mkdocs build --strict # what CI runs: fails on broken links and missing excerpts
```

## The offline package

amadeOS installs the handbook as the Debian package `amade-app-handbook`: the pages in
`/usr/share/doc/amade-app-handbook/html` and a desktop entry of category `Documentation`, so the
Help drawer of the Front Panel lists *Application Developer's Handbook* and opens it in the web browser.

```bash
./gradlew installToAmadeOs        # builds the package and copies it to ../amadeos/config/packages.chroot
docs/handbook/package-deb.sh 0.1.0 /tmp   # the package alone
```

It is built from `mkdocs-offline.yml`, which takes everything from `mkdocs.yml` and adds what reading
from disk needs: links to `.html` files (`use_directory_urls: false`), the `offline` plugin for search
without a server, and the `privacy` plugin, which stores the fonts and scripts (Mermaid) that the
pages would load from the web. The applications must be checked out next to `amade-commons` as for the
published book, but the excerpts are taken at the commits `handbook.yml` pins (fetched if missing), so
the package builds whatever branch the checkouts are on; MkDocs is installed into `.venv` if none is on the PATH. The package version is the
library's version plus the date of the last commit to `docs/handbook`. CI builds this variant too.
