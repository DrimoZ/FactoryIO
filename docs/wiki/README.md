# Wiki source

These files are the **source** of the [GitHub wiki](https://github.com/DrimoZ/FactoryIO/wiki).
They live in the repository so that a change to the code and the change to its documentation
travel in the same commit — a wiki edited only through the web interface drifts, and nothing
signals it.

GitHub wikis are a separate git repository. To publish:

```bash
git clone https://github.com/DrimoZ/FactoryIO.wiki.git /tmp/fio-wiki
cp docs/wiki/*.md /tmp/fio-wiki/
rm /tmp/fio-wiki/README.md
cd /tmp/fio-wiki && git add -A && git commit -m "Sync from docs/wiki" && git push
```

The wiki must have been initialised once through the web interface — GitHub does not create the
`.wiki.git` repository until the first page exists.

`_Sidebar.md` drives the navigation on every page. Page names come from file names: a link to
`[Transport Belts](Transport-Belts)` resolves to `Transport-Belts.md`.

This `README.md` is not copied — it describes the folder, not the mod.
