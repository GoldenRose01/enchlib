"""Build the GitHub Wiki language editions using only the Python standard library.

Run: python docs/wiki-i18n/generate.py [--check]
Italian pages remain the source for technical tables and executable examples.
Translated prose lives beside this script; generated pages use flat GitHub Wiki names.
"""
from pathlib import Path
import argparse
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[2]
WIKI = ROOT / 'docs/wiki'
DATA = Path(__file__).resolve().parent
SLUGS = ['Home', 'Installazione', 'Incantamenti', 'Configurazione',
         'Commerci-dei-villager', 'Novita-1.7.0', 'Tavoli-e-rarita',
         'Categorie-mob', 'Comandi', 'API', 'Eldrich-Surge', 'FAQ', 'Fonti']
LANGS = {'en': 'English', 'es': 'Español', 'zh': '简体中文', 'hi': 'हिन्दी',
         'ar': 'العربية', 'pt': 'Português', 'bn': 'বাংলা', 'ru': 'Русский', 'fr': 'Français'}
LINK = re.compile(r'(?<!!)\[[^\]]*\]\(([^)]+)\)')
FENCES = re.compile(r'```[^\n]*\n.*?```', re.S)


def table(headers, rows):
    return '\n'.join(['| ' + ' | '.join(headers) + ' |',
                      '|' + '|'.join(['---'] * len(headers)) + '|'] +
                     ['| ' + ' | '.join(row) + ' |' for row in rows])


def source_tables(text):
    groups = re.findall(r'(?:^\|.*\n?)+', text, re.M)
    return [[list(map(str.strip, line.strip().strip('|').split('|')))
             for line in g.splitlines()][2:] for g in groups]


def switch(slug):
    return ' · '.join([f'[Italiano]({slug})'] +
                      [f'[{name}]({lang}-{slug})' for lang, name in LANGS.items()])


def render():
    sources = {s: (WIKI / (s + '.md')).read_text(encoding='utf-8') for s in SLUGS}
    presets = sorted(WIKI.glob('Preset-*.md'))
    assert len(presets) == 42
    outputs = {}
    for lang, name in LANGS.items():
        d = json.loads((DATA / (lang + '.json')).read_text(encoding='utf-8'))
        assert len(d['titles']) == len(d['body']) == 13, lang
        assert len(d['effects']) == 42 and len(d['commands']) == 19, lang
        assert len(d['files']) == 9 and len(d['categories']) == 11, lang
        titles = dict(zip(SLUGS, d['titles']))
        labels = d['labels']
        assert len(labels) == 18, lang
        nav = ' · '.join(f'[{titles[s]}]({lang}-{s})' for s in ['Home', 'Incantamenti', 'Configurazione', 'Comandi', 'API'])

        def page(slug, title, body):
            # Keep the Markdown outside HTML blocks so GitHub renders it normally.
            heading = f'# {title}\n\n'
            return heading + switch(slug) + '\n\n' + nav + '\n\n---\n\n' + body.strip() + '\n'

        for index, slug in enumerate(SLUGS):
            body = d['body'][index]
            body = re.sub(r'\{link:([^}]+)\}', lambda m: f'[{titles[m[1]]}]({lang}-{m[1]})', body)
            if slug == 'Home':
                body += '\n\n' + '\n'.join(f'- [{titles[s]}]({lang}-{s})' for s in SLUGS[1:])
            if slug == 'Installazione':
                rows = source_tables(sources[slug])[0]
                keys = ['Minecraft', 'EnchLib', 'JAR', 'Fabric Loader', 'Fabric API', 'Fabric Language Kotlin', 'Java']
                body += '\n\n' + table([labels[0], '26.3', '26.4 Snapshot 7'], [[k] + r[1:] for k, r in zip(keys, rows)])
            if slug == 'Configurazione':
                rows = source_tables(sources[slug])[0]
                body += '\n\n' + table([labels[0], labels[1]], [[r[0], desc] for r, desc in zip(rows, d['files'])])
            if slug == 'Categorie-mob':
                rows = source_tables(sources[slug])[0]
                body += '\n\n' + table([labels[0], labels[1]], [[r[0], desc] for r, desc in zip(rows, d['categories'])])
            if slug == 'Comandi':
                rows = sum(source_tables(sources[slug]), [])
                body += '\n\n' + table([labels[0], labels[1]], [[r[0], desc] for r, desc in zip(rows, d['commands'])])
            if slug == 'Tavoli-e-rarita':
                body += '\n\n' + table([labels[5], labels[14], '`chance = 80`'], source_tables(sources[slug])[1])
            if slug == 'API':
                rows = source_tables(sources[slug])[0]
                assert len(d['api']) == len(rows)
                # Preserve exact method identifiers; translate the two prose fragments.
                body += '\n\n' + table([labels[0], 'API'], [[desc, r[1].replace('relativi setter', d['setters']).replace('getter dei preset', d['getters'])] for r, desc in zip(rows, d['api'])])
            if slug == 'Incantamenti':
                rows = source_tables(sources[slug])[0]
                translated = []
                for r, effect in zip(rows, d['effects']):
                    translated.append([re.sub(r'\]\((Preset-[^)]+)\)', rf']({lang}-\1)', r[0]), effect] + r[2:])
                body += '\n\n' + table([labels[2], labels[1], labels[4], labels[5], labels[6]], translated)
            # Executable examples are kept byte-for-byte, including their source comments.
            examples = FENCES.findall(sources[slug])
            if examples:
                body += '\n\n## ' + labels[15] + '\n\n' + '\n\n'.join(examples)
            if slug == 'Fonti':
                paths = re.findall(r'`([^`]+\.(?:kt|java|md|json|properties|config))`', sources[slug])
                body += '\n\n' + '\n'.join('- `' + p + '`' for p in paths)
            links = []
            for url in LINK.findall(sources[slug]):
                if url.startswith('https://') and url not in links:
                    links.append(url)
            if links:
                body += '\n\n## ' + labels[8] + '\n\n' + '\n'.join(f'- [{url.rsplit("/", 1)[-1]}]({url})' for url in links)
            outputs[f'{lang}-{slug}.md'] = page(slug, titles[slug], body)

        for i, path in enumerate(presets):
            src = path.read_text(encoding='utf-8')
            title = src.splitlines()[0].removeprefix('# ')
            rows = source_tables(src)[0]
            fields = [labels[3], labels[4], labels[5], labels[6], labels[7]]
            values = [row[1].replace('Nessuna voce', labels[9]) for row in rows]
            body = d['effects'][i] + '\n\n'
            if 'curse-of-' in path.stem:
                old = 'curse_of_binding' if 'binding' in path.stem else 'curse_of_vanishing'
                new = 'binding_curse' if 'binding' in path.stem else 'vanishing_curse'
                body += '> ' + d['legacy'].replace('{old}', 'minecraft:' + old).replace('{new}', 'minecraft:' + new) + '\n\n'
            body += table([labels[10], labels[11]], list(zip(fields, values)))
            body += '\n\n## ' + labels[12] + '\n\n' + d['preset']
            body += '\n\n## ' + labels[8] + '\n\n' + '\n'.join(
                f'- [{label}]({url})' for label, url in zip(
                    [labels[4], labels[6], labels[7], labels[13]],
                    LINK.findall(src)[-4:]))
            outputs[f'{lang}-{path.stem}.md'] = page(path.stem, title, body)
    outputs['Languages.md'] = '# Languages / Lingue\n\n' + switch('Home') + '\n\n' + table(
        ['Language / Lingua', 'Wiki', 'Pages / Pagine'],
        [['Italiano', '[Home](Home)', '55']] + [[name, f'[{name}]({lang}-Home)', '55'] for lang, name in LANGS.items()]) + '\n'
    return outputs


def validate(outputs):
    pages = {p.stem for p in WIKI.glob('*.md')} | {Path(p).stem for p in outputs}
    for name, content in outputs.items():
        assert '\ufffd' not in content, f'Invalid Unicode: {name}'
        assert not re.search(r'\{(?:link:|old|new)', content), name
        assert content.count('```') % 2 == 0, name
        for target in LINK.findall(content):
            if not target.startswith(('https://', 'http://', '#')):
                assert target.split('#')[0] in pages, f'Broken link: {name} -> {target}'
    # Each language has all 55 pages, and every original code block is retained.
    for lang in LANGS:
        for slug in SLUGS:
            original = (WIKI / f'{slug}.md').read_text(encoding='utf-8')
            assert FENCES.findall(original) == FENCES.findall(outputs[f'{lang}-{slug}.md']), (lang, slug)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    outputs = render()
    validate(outputs)
    stale = []
    for name, content in outputs.items():
        path = WIKI / name
        if args.check:
            if not path.exists() or path.read_text(encoding='utf-8') != content:
                stale.append(name)
        else:
            path.write_text(content, encoding='utf-8', newline='\n')
    if stale:
        print('Out-of-date pages: ' + ', '.join(stale))
        return 1
    print(f'{"Verified" if args.check else "Generated"} {len(outputs)} pages; {len(LANGS)} languages, 42 presets per language; links and code examples valid.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
