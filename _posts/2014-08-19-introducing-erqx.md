---
title: "Introducing ERQX"
tags: scala play erqx
---

Today I migrated my blog to a new blogging engine that I've written called [ERQX](https://github.com/jroper/erqx).  Now to start off with, why did I write my own blog engine?  A case of not invented here syndrome?  Or do I just really like writing blog engines (I was, technically still am, the lead developer of [Pebble](http://pebble.sf.net), the blog that I used to use)?

I was very close to migrating to a Jekyll blog hosted on GitHub, but there are a few reasons why I didn't do this:

* As a full time maintainer of Play, I don't get a lot of opportunities to use Play as an end user.  This is bad, how can I be expected to guide Play forward if I don't feel the pain points as an end user?  Hence, I jump at every opportunity I can to write new apps in it, and what better use case is there than my own blog?
* I really like the setup we have with the documentation on the [Play website](https://playframework.com) - we have implemented some custom markdown extensions that allow extracting code snippets from compiled and tested source files, and all documentation is served directly out of git, which turns out to be a great way to deploy and distribute content.
* I wanted to see how easy it would be to make a full reusable and skinnable application within Play.
* Because I love Play!

## Features

So what are the features of ERQX?  Here are a few:

### Embedabble

The blog engine is completely embeddable.  All you need to do is add a single line to your `routes` file to include the blog router, and some configuration in `application.conf` pointing to a git repository, and you're good to go.

Not convinced?  Here is everything you need to do to include a blog in your existing Play application.

1. Add a dependency to your `build.sbt` file:

        resolvers += "ERQX Releases" at "https://jroper.github.io/releases"
        
        libraryDependencies += "au.id.jazzy.erqx" %% "erqx-engine" % "1.0.0"

2. Add the blog router to your `routes` file:

        ->  /blog       au.id.jazzy.erqx.engine.controllers.BlogsRouter

3. Add some configuration pointing to the git repo for your blog:

        blogs {
          default {
            gitConfig {
              gitRepo = "/path/to/some/repo"
              remote = "origin"
              fetchKey = "somesecret"
            }
          }
        }

And there you have it!

### Git backend

In future I hope to add other backends, I think a [prismic.io](http://prismic.io) backend would be really cool, but for now it just supports a git backend.  The layout of the git repo is somewhat inspired by Jekyll, blog posts go in a folder named `_posts`, named with the date and title in the name, and each blog post has a front matter in yaml format.  Blog posts can either be in markdown or HTML format.  There is also a `_config.yml` file which contains configuration for the blog, such as the title, description and a few other things.

Changes are deployed to the blog either by polling, or by registering a commit hook on GitHub.  In the example adove, the url for the webhook would be `http://example.com/blog/fetch/somesecret`.  Using commit hooks, blog posts are published within seconds of pushing to GitHub.  ERQX also takes advantage of the git hash, serving that as the ETag for all content, allowing caching of the blog and its associated resources.

### Markdown

Blog posts can be in markdown format, and uses the [Play documentation renderer](https://github.com/playframework/play-doc) to support pulling code samples out of compiled and tested source files.  This is invaluable if you write technical blog posts full of code and you want to ensure that the code in the blog post works.

### Themeable

The blog is completely themeable, allowing you to simply override the header and footer to plug in different stylesheets, or completely use your own templates to render blog posts.

The default theme uses a pure CSS responsive layout, switching to rendering the description of the blog in a slideout tab on mobile devices, and provides support for comments via [Disqus](https://disqus.com).

### Multi blog support

ERQX allows serving multiple blogs from the one server.  Each may have its own theme.

## Source code and examples

ERQX and its associated documentation can be found [on GitHub](https://github.com/jroper/erqx).

The website for this blog, showing how the blog can be emedded in a real application, plus the content of the blog itself, can also be found [on GitHub](https://github.com/jroper/jazzyidau).  The website is in the `master` branch, while the blog content is in the `allthatjazz` branch.
