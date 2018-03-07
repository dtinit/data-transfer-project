---
layout: page
title: Overview of GitHub Pages
description: Overview of construction of a website with GitHub Pages
---

The present site is simple, with a style derived from
[JekyllBootstrap](https://jekyllbootstrap.com/) and
[Twitter Bootstrap](https://getbootstrap.com) with a particular
theme. I'll explain how to create a site with exactly this style. If
you want something else, try the
[GitHub Pages](https://pages.github.com) automatic site generator, or
look at the [resources page](pages/resources.html).

These GitHub Pages sites are constructed by having a `gh-pages` branch
of a GitHub repository, with specific files layed out in a specific
way. To see the structure of such a repository, look at the
[repository for the present site](https://github.com/kbroman/simple_site).

    _includes/
    _layouts/
    _plugins/
    assets/
    pages/
    .gitignore
    License.md
    Rakefile
    ReadMe.md
    _config.yml
    index.md

The directories beginning with an underscore contain materials
defining the basic layout and style for the site. If you
[build the site locally](pages/local_test.html) (for testing
purposes), there will also be a `_site/` directory containing the
actual site (with
[Markdown](https://daringfireball.net/projects/markdown/) files
converted to html). You don't want the `_site/` directory in your
repository, so include that in the `.gitignore` file.

The
[`assets/`](https://github.com/kbroman/simple_site/tree/gh-pages/assets)
directory contains any non-Markdown materials for the site (e.g.,
images or example code). These files won't be touched in the
conversion but will be just copied over as-is.

The
[`pages/`](https://github.com/kbroman/simple_site/tree/gh-pages/pages)
directory contains
[Markdown](https://daringfireball.net/projects/markdown/) files that
will become html pages on your site.

The
[`_config.yml`](https://github.com/kbroman/simple_site/blob/gh-pages/_config.yml)
file contains all sorts of configuration parameters (some of which
you'll need to modify). The [`Rakefile`](https://github.com/kbroman/simple_site/blob/gh-pages/Rakefile) contains instructions for
the conversion; you won't modify this file.

It's best to always include
[`License.md`](https://github.com/kbroman/simple_site/tree/gh-pages/License.md)
and
[`ReadMe.md`](https://github.com/kbroman/simple_site/tree/gh-pages/ReadMe.md)
files. But you wouldn't need these to be placed on the website; they'd
just be viewed in the repository on [GitHub](https://github.com). The
[`_config.yml`](https://github.com/kbroman/simple_site/tree/gh-pages/_config.yml)
file contains
[a line sort of like the following](https://github.com/kbroman/simple_site/blob/gh-pages/_config.yml#L5)
(but listing a few more files), indicating files to _not_ move to the
final site.

    exclude: ["ReadMe.md", "Rakefile", "License.md"]

Finally,
[`index.md`](https://raw.githubusercontent.com/kbroman/simple_site/gh-pages/index.md)
is the Markdown version of the main page for your site.

The
[`index.md`](https://raw.githubusercontent.com/kbroman/simple_site/gh-pages/index.md)
file and the Markdown files in
[`pages/`](https://github.com/kbroman/simple_site/blob/gh-pages/pages)
(e.g.,
[the present page](https://raw.githubusercontent.com/kbroman/simple_site/gh-pages/pages/overview.md))
have a header with a particular form:

    ---
    layout: page
    title: simple site
    tagline: Easy websites with GitHub Pages
    description: Minimal tutorial on making a simple website with GitHub Pages
    ---

In the conversion of the site from Markdown to html, this bit says
that the current file is to be converted with the &ldquo;page&rdquo;
layout, and gives the title and the (optional) &ldquo;tagline.&rdquo;
The "`description:`" part gets converted into
`<meta name="description" content="Minimal tutorial on...">`
which, in principle, may be used in the results of google searches.

The rest is basically plain Markdown, though the present site is
configured to use [kramdown](https://kramdown.gettalong.org/) as the
Markdown converter (via
[this line in the `_config.yml` file](https://github.com/kbroman/simple_site/blob/gh-pages/_config.yml#L23)).
Read about the [kramdown syntax](https://kramdown.gettalong.org/syntax.html)
or just look at the
[quick reference](https://kramdown.gettalong.org/quickref.html).

Now go to the page about [how to make an independent website](independent_site.html).
