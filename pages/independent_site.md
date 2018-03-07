---
layout: page
title: Making an independent website
description: How to make an independent website with GitHub Pages.
---

This is what to do if you just want a website. (This page is a bit
long, but it's really not that much work.)

### First things

Start by cloning
[the repository for the present site](https://github.com/kbroman/simple_site). (Or,
alternatively, fork it and then clone your own version.)

    git clone git://github.com/kbroman/simple_site

Then change the name of that directory to something meaningful.

    mv simple_site something_meaningful

(Of course, don't use `something_meaningful` but rather
_something meaningful_.)

Now change into that directory and remove the `.git` directory
(because you don't want the history of _my_ repository).

    cd something_meaningful
    rm -r .git

Now make it a git repository again.

    git init

### Things not to change

You'll need to keep the following files and directories largely unchanged.

    Rakefile
    _includes
    _layouts
    _plugins
    assets/themes

We _will_ change one file within `_includes/`; see below.

### Edit the `_config.yml` file

The
[`_config.yml`](https://github.com/kbroman/simple_site/blob/gh-pages/_config.yml)
file contains a bunch of configuration information. You'll want to
edit this file to replace my information with your information.

Perhaps edit the
[line with `exclude:`](https://github.com/kbroman/simple_site/blob/gh-pages/_config.yml#L5)
if you've named `License.md` and/or `ReadMe.md` differently. (I've
edited this line a bit, here.)

    exclude: [..., "ReadMe.md", "Rakefile", "License.md"]

Edit the
[lines about the site name and author](https://github.com/kbroman/simple_site/blob/gh-pages/_config.yml#L11-L17).

    title : simple site
    author :
      name : Karl Broman
      email : kbroman@gmail.com
      github : kbroman
      twitter : kwbroman
      feedburner : nil

Edit the
[`production_url` line](https://github.com/kbroman/simple_site/blob/gh-pages/_config.yml#L19)
by replacing `kbroman` with _your_ github user name, and replace
`simple_site` with the name that your repository will have on github
(`something_meaningful`?).

    production_url : https://kbroman.github.io/simple_site

Note that the `https` (vs `http`) is important here; see
&ldquo;[Securing your github pages site with https](https://help.github.com/articles/securing-your-github-pages-site-with-https/).&rdquo;
(I need to use `http` because my site uses the custom domain
`kbroman.org`, but you likely need `https`.)

Replace the
[`BASE_PATH` line](https://github.com/kbroman/simple_site/blob/gh-pages/_config.yml#L52)
with the same url.

    BASE_PATH : https://kbroman.github.io/simple_site

There's also an
[`ASSET_PATH` line](https://github.com/kbroman/simple_site/blob/gh-pages/_config.yml#L62),
but you can leave that commented-out (with the `#` symbol at the beginning).

Note that for the `BASE_PATH`, I actually have
`http://kbroman.org/` in place of `https://kbroman.github.io/`. I set up
a
[custom domain](https://help.github.com/articles/setting-up-a-custom-domain-with-github-pages),
which involved a series of emails with a DNS provider. I
don't totally understand how it works, and I'm not _entirely_ sure
that I've done it right. But if you want to have a custom domain, take
a look at
[that GitHub help page](https://help.github.com/articles/setting-up-a-custom-domain-with-github-pages).

### Edit `_includes/themes/twitter/default.html`

The
[`_includes/themes/twitter/default.html`](https://github.com/kbroman/simple_site/blob/gh-pages/_includes/themes/twitter/default.html)
file defines how a basic page will look on your site. In particular,
it contains a bit of html code for a footer, if you want one.

Find the
[footer for my site](https://github.com/kbroman/simple_site/blob/gh-pages/_includes/themes/twitter/default.html#L47-L50)
and remove it or edit it to suit. This is the only bit of html you'll
have to deal with.

    <!-- start of Karl's footer; modify this part -->
        <a href="https://creativecommons.org/publicdomain/zero/1.0/">  ...
        <a href="http://kbroman.org">Karl Broman</a>
    <!-- end of Karl's footer; modify this part -->

### Edit or remove the Markdown files

Edit the
[`index.md`](https://raw.githubusercontent.com/kbroman/simple_site/gh-pages/index.md)
file, which will become the main page for your site.

First, edit the initial chunk with a different title and tagline. Feel
free to just delete the tagline.

    ---
    layout: page
    title: simple site
    tagline: Easy websites with GitHub Pages
    ---

Now edit the rest (or, for now, just remove) the rest of the file.

Now go into the [`pages/`](https://github.com/kbroman/simple_site/blob/gh-pages/pages) directory and remove or rename and modify
all of the Markdown files in there

Note that when you link to any of these Markdown-based pages, you'll
want to use a `.html` extension rather than `.md`. For example, look
at the
[main page](https://raw.githubusercontent.com/kbroman/simple_site/gh-pages/index.md)
for this site; the links in the bullet points for the various pages
look like this:

    - [Overview](pages/overview.html)
    - [Making an independent website](pages/independent_site.html)
    - [Making a personal site](pages/user_site.html)
    - [Making a site for a project](pages/project_site.html)
    - [Making a jekyll-free site](pages/nojekyll.html)
    - [Testing your site locally](pages/local_test.html)
    - [Resources](pages/resources.html)

### Commit all of these changes.

At the start, we'd removed the `.git/` subdirectory (with the history
of _my_ repository) and then used `git init` to make it a new git
repository.

Now you want to add and commit all of the files, as modified.

    git add .
    git commit -m "Initial commit"

Then change the name of the master branch to `gh-pages`.

    git branch -m master gh-pages

### Push everything to GitHub

Now go back to GitHub and create a new repository, called something
meaningful. (I'll again pretend that it's explicitly
`something_meaningful`.)

Then go back to the command line and push your repository to
[GitHub](https://github.com).

    git remote add origin git@github.com:username/something_meaningful

Replace `username` with your GitHub user name and
`something_meaningful` with the name of your repository. And you might
want to use the `https://` construction instead, if you're not using ssh.

    git remote add origin https://github.com/username/something_meaningful

Finally, push everything to GitHub.

    git push -u origin gh-pages

Note that we're using `gh-pages` and not `master` here, as we want
this stuff in a `gh-pages` branch.

### Check whether it worked

Go to `https://username.github.io/something_meaningful` and cross your
fingers that it worked. (Really, _I_ should be crossing my fingers.)

### Up next

Now go to [making a personal site](user_site.html).
