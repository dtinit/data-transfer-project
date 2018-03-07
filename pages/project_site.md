---
layout: page
title: Making a project site
description: Using GitHub Pages to make a webpage for a GitHub-hosted project
---

If you want to make a website for a GitHub-hosted project, as I've
done for my [R/qtlcharts package](http://kbroman.org/qtlcharts),
you follow my
[instructions for making an independent site](independent_site.html),
with just a few modifications.

Go to your local repository and create and switch to an
&ldquo;orphan&rdquo; `gh-pages` branch. (It's an &ldquo;orphan&rdquo;
branch because it won't contain the whole history of your project.)


    cd my_repo
    git checkout --orphan gh-pages

Remove _everything_.

    git rm -rf .

Now go back one directory and clone
[the present repository](https://github.com/kbroman/simple_site).

    cd ..
    git clone git://github.com/kbroman/simple_site

Change into that directory and remove the `.git/` directory.

    cd simple_site
    \rm -rf .git

Move all of the stuff from that directory into _your_ repository
(in the new and empty `gh-pages` branch).

    cd ../my_repo
    cp -r ../simple_site/. .

Edit everything [as before](independent_site.html).
Commit everything and push the `gh-pages` branch to github.

    git add .
    git commit -m "Initial commit of web site"
    git push origin gh-pages

Now you'll switch back-and-forth between the `gh-pages` branch (to
edit your website) and the `master` or other branchs (to edit your
project).

Personally, I'll clone a separate copy of my repository, one directory
up, called `Web/`, that is sitting in the `gh-pages` branch. Then
rather than using `git checkout` to switch between the code and the
web, I switch from one directory to another.

### Up next

Now go to [making a jekyll-free site](nojekyll.html) or
[testing your site locally](local_test.html).
