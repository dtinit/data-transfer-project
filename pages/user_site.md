---
layout: page
title: Making a personal site
description: How to make a personal web page with GitHub Pages
---

Your [GitHub Pages](https://pages.github.com) sites will appear at

    https://username.github.io/some_site

Of course, this will be with _your_ GitHub user name and with the
names of your GitHub repositories.

I'd recommend putting _something_ at `https://username.github.io`,
since people might look there. (When I started with GitHub Pages, I
thought you were _required_ to have such a site, but either they've
changed things or I'm just mistaken; you don't _need_ this anymore.)

You create one of these sites in much the same way as you
[create an independent GitHub Pages site](independent_site). The only
real differences are

- The repository needs to be called `username.github.io`
- The site sits in the `master` branch rather than the `gh-pages` branch.

_My_ personal site, [kbroman.github.io](https://kbroman.github.io)
(which shows up as [kbroman.org](http://kbroman.org); see the
[GitHub help page on setting up a custom domain](https://help.github.com/articles/setting-up-a-custom-domain-with-github-pages))
is minimal and is actually written in straight html rather than
[Markdown](https://daringfireball.net/projects/markdown/). If you
want, you could just make an edited version of my site:

- Clone my
  [kbroman.github.io repository](https://github.com/kbroman/kbroman.github.io)
- Remove the `.git` directory
- Edit `index.html`, `404.html`, `README.md`, and `License.md`
- Use `git init`, `git add`, `git commit`
- Create a new repository on GitHub named `username.github.io`
- Go back to the command line and do `git remote add` and
  `git push -u origin master`

Alternatively, you could use the procedure I described for
[making an independent website](independent_site.html). The only thing
you do differently is to use the `master` branch rather than a
`gh-pages` branch.

A final note: the `404.html` file will serve as the &ldquo;page not
found&rdquo; page for _all_ of your GitHub Pages (that is, if you
_want_ a personalized 404 page).

### Up next

Now go to [making a project site](project_site.html).
