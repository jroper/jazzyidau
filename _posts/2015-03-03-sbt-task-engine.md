---
title: "sbt - A task engine"
tags: scala sbt
---

sbt is the best build tool that I've used.  But it's also the build tool with the steepest learning curve that I've ever used, and I think most people would agree that it's very difficult to learn.  When you first start using it, configuring it is like casting spells, spells that have to be learned from a spell book, that have to be said in the exact right way, otherwise they don't work.  There are lots of guides out there that are essentially spell books, they teach you all the things you need to know to achieve various tasks.  But I haven't seen a lot out there that actually explains what sbt is, what it does, why it is the way it is.  This blog post is my attempt to do that.

## A task engine

Simply put, sbt is a task engine.  You have tasks.  A task may be dependent on other tasks.  Any task from any point in the build may be redefined, and new tasks can be easily added.  In some ways it is a bit like make or ant, but it differs in a fundamental way, sbt tasks produce an output value, and are able to consume the output values of the tasks they depend on - whereas make and ant just modify the file system.  This property of sbt allows you to break build steps up into very fine grained tasks.

So let's take an example.  A common step that build tools support is compilation.  In many traditional build tools, a compilation task is responsible for finding a set of files to compile based on some input parameters, such as a list of source directories, and compiling them.  In sbt, the `compile` task is not responsible for finding a set of files to compile, this is the responsibility of the `sources` task.  The output value of the sources task is a list of files to compile.  The `compile` task depends on the `sources` task, taking its list of files to compile as an input.

So what's so good about this?  What it means is that I can completely customise the way sources are located, by redefining the `sources` task.  So if I have a crazy build requirement such as wanting to put a special annotation in my source files to say whether they get compiled or not, I can very easily implement my own `sources` task to do that.  This is something that would be very difficult to do in another build tool, but in sbt it's straight forward.

In other build tools, if I want to generate some sources, I have to make sure that the task to generate the sources runs before the compilation task, and puts them in a place the compilation task will find.  In sbt, I can just redefine the `sources` task to make it generate the sources.  In practice though, I don't need to do that, because generating sources is a very common requirement.  Remember that I said that sbt tasks can be very fine grained.  The `sources` task itself depends on many other tasks, one of them is the `managedSources` task, which collects all the files that are managed (or generated) by the build (in contrast to unmanaged sources, which are your regular source files that you manage yourself).  That task in turn depends on the `sourceGenerators` task, which I can redefine to add new source generators.

## A self documenting task engine

At this point you might be starting to see that there are many, many tasks involved in even the simplest build in sbt.  I've talked about just one small part, how generated sources end up being compiled, but there are many more than that.  How is someone that is new to sbt supposed to know what tasks exist, so that they can customise their build?  Well, it turns out sbt comes with a few built in tools for inspecting the available tasks.  These are often seen as advanced features of sbt, but I think really this is what new users to sbt should be introduced to first.  So if you're new, its time to fire up sbt.

First we need a simple project.  In an empty directory, create a file called `build.sbt`, and set your projects name:

```scala
name := "sbt-fun"
```

Now, if you already have sbt 0.13 or later installed, you can use that.  If you already have activator installed - which is basically just a script that launches sbt, then you can use that.  If you have neither, then go [here](https://typesafe.com/community/core-tools/activator-and-sbt) and download activator or sbt, it doesn't matter which, and install it, and then start it in your projects directory:

```
$ sbt
[info] Loading project definition from /Users/jroper/sbt-fun/project
[info] Updating {file:/Users/jroper/sbt-fun/project/}sbt-fun-build...
[info] Resolving org.fusesource.jansi#jansi;1.4 ...
[info] Done updating.
[info] Set current project to sbt-fun (in build file:/Users/jroper/sbt-fun/)
> 
```

So, now we're on the sbt console.  Earlier we were talking about the `sources` task.  Let's have a look at it.  sbt has a command called `inspect`, which lets you inspect a task:

```
> inspect sources
[info] Task: scala.collection.Seq[java.io.File]
[info] Description:
[info]  All sources, both managed and unmanaged.
[info] Provided by:
[info]  {file:/Users/jroper/sbt-fun/}sbt-fun/compile:sources
[info] Defined at:
[info]  (sbt.Defaults) Defaults.scala:188
[info] Dependencies:
[info]  compile:unmanagedSources
[info]  compile:managedSources
[info] Delegates:
[info]  compile:sources
[info]  *:sources
[info]  {.}/compile:sources
[info]  {.}/*:sources
[info]  */compile:sources
[info]  */*:sources
[info] Related:
[info]  test:sources
```

What are we looking at?  First, we can see that `sources` is a task that produces a sequence of files - as I said before.  We can also see a description of the task, *All sources, both managed and unmanaged.*  The *Defined at* section is interesting, it shows us where the `sources` task is defined, in this case, it's on [line 188 of the sbt `Defaults` class](https://github.com/sbt/sbt/blob/0.13.5/main/src/main/scala/sbt/Defaults.scala#L188).  We can see that it has two tasks that it depends on, `unmanagedSources` and `managedSources`.  The rest of the information we won't worry about for now.

Now before we start playing with our build, we can actually get even more information here, not only is it possible to inspect a single task in sbt, you can also inspect a whole tree of tasks, using the `inspect tree` command:

```
> inspect tree sources
[info] compile:sources = Task[scala.collection.Seq[java.io.File]]
[info]   +-compile:unmanagedSources = Task[scala.collection.Seq[java.io.File]]
[info]   | +-*/*:sourcesInBase = true
[info]   | +-*/*:excludeFilter = sbt.HiddenFileFilter$@5a63fa71
[info]   | +-*:baseDirectory = /Users/jroper/sbt-fun
[info]   | +-*/*:unmanagedSources::includeFilter = sbt.SimpleFilter@44a44a04
[info]   | +-compile:unmanagedSourceDirectories = List(/Users/jroper/sbt-fun/src/main/scala, /Users/jroper/sbt-fun/sr..
[info]   |   +-compile:javaSource = src/main/java
[info]   |   | +-compile:sourceDirectory = src/main
[info]   |   |   +-*:sourceDirectory = src
[info]   |   |   | +-*:baseDirectory = /Users/jroper/sbt-fun
[info]   |   |   |   +-*:thisProject = Project(id sbt-fun, base: /Users/jroper/sbt-fun, configurations: List(compile,..
[info]   |   |   |   
[info]   |   |   +-compile:configuration = compile
[info]   |   |   
[info]   |   +-compile:scalaSource = src/main/scala
[info]   |     +-compile:sourceDirectory = src/main
[info]   |       +-*:sourceDirectory = src
[info]   |       | +-*:baseDirectory = /Users/jroper/sbt-fun
[info]   |       |   +-*:thisProject = Project(id sbt-fun, base: /Users/jroper/sbt-fun, configurations: List(compile,..
[info]   |       |   
[info]   |       +-compile:configuration = compile
[info]   |       
[info]   +-compile:managedSources = Task[scala.collection.Seq[java.io.File]]
[info]     +-compile:sourceGenerators = List()
[info]     
```

So in here you can see that `sources` to `managedSources` to `sourceGenerators` chain that I mentioned before, and you can also see the `unmanagedSources` chain, which is a lot more complex, we can see directory hierarchies, filters for deciding which files to include and exclude, etc.

## Settings vs Tasks

At this point you may notice that there are two types of tasks in the tree, there are things like `managedSources`, which just describe the type of the task:

    compile:managedSources = Task[scala.collection.Seq[java.io.File]]

And then there are things like `scalaSource`, which actually display a value:

    compile:scalaSource = src/main/scala

This is actually an sbt optimisation, sbt has a special type of task called a `Setting`.  Settings get executed once per session, so when you start sbt up, you start a new session, and all the settings get executed then.  This is why when I inspect the tree, sbt can show me the value, because it already knows it.  In contrast, an ordinary `Task` gets executed once per execution.  So if I now run the `sources` task, that `managedSources` task will be executed then.  If I run `sources` again, it will be executed again.  But my settings only get executed once for the whole session.

It should be noted that an *execution* is a request by the user to execute a task.  If two tasks in my tree depend on the `sources` task twice, sbt will ensure that the `sources` task only gets executed once.  So if I run the `publish` task, which transitively depends on the `compile` task, as well as the `doc` task (that generates java/scala docs), and the `packageSrc` task (that generates source jars), these all depend on the same `sources` task, which will only be executed once during my `publish` execution, and the value will be reused as the input for all three tasks.

Now naturally, since settings are executed at the start of the session, and not as part of an execution, they can't depend on tasks, they can only depend on other settings.  Meanwhile, tasks can depend on both other tasks and settings.

When it's important to know the difference between settings and tasks is when you're writing your own sbt plugins that define their own settings and tasks.  But in general, you can consider them to be the same thing, settings are just a small optimisation so that they don't have to be executed every time.  When defining your own tasks or settings, a good rule of thumb is if in doubt, just define a task.

## Scopes

Scopes are another important feature of the sbt task engine.  A task can be scoped.  When a task depends on another task, it can depend on that task in a particular scope.  Now one obvious type of scope that sbt supports is the configuration scope.  sbt has a few built in configurations, the two main ones that you'll interact with are `compile` and `test`.  So above, when the `sources` command depends on `managedSources`, you can see that it actually depends on `compile:managedSources`, which means it depends on `managedSources` in the `compile` scope.

In actual fact, you can see at the top that we are looking at the tree for `compile:sources`.  When you don't specify a scope, sbt will choose a default scope, in this case it has chosen the `compile` scope.  The logic in how it makes that decision we won't cover here.  We could also inspect the `test:sources` tree:

```
> inspect tree test:sources
[info] test:sources = Task[scala.collection.Seq[java.io.File]]
[info]   +-test:unmanagedSources = Task[scala.collection.Seq[java.io.File]]
[info]   | +-test:unmanagedSourceDirectories = List(/Users/jroper/sbt-fun/src/test/scala, /Users/jroper/sbt-fun/src/t..
[info]   | | +-test:javaSource = src/test/java
[info]   | | | +-test:sourceDirectory = src/test
[info]   | | |   +-*:sourceDirectory = src
[info]   | | |   | +-*:baseDirectory = /Users/jroper/sbt-fun
[info]   | | |   |   +-*:thisProject = Project(id sbt-fun, base: /Users/jroper/sbt-fun, configurations: List(compile,..
[info]   | | |   |   
[info]   | | |   +-test:configuration = test
[info]   | | |   
[info]   | | +-test:scalaSource = src/test/scala
[info]   | |   +-test:sourceDirectory = src/test
[info]   | |     +-*:sourceDirectory = src
[info]   | |     | +-*:baseDirectory = /Users/jroper/sbt-fun
[info]   | |     |   +-*:thisProject = Project(id sbt-fun, base: /Users/jroper/sbt-fun, configurations: List(compile,..
[info]   | |     |   
[info]   | |     +-test:configuration = test
[info]   | |     
[info]   | +-*/*:unmanagedSources::includeFilter = sbt.SimpleFilter@44a44a04
[info]   | +-*/*:excludeFilter = sbt.HiddenFileFilter$@5a63fa71
[info]   | 
[info]   +-test:managedSources = Task[scala.collection.Seq[java.io.File]]
[info]     +-test:sourceGenerators = List()
[info]     
```

It looks pretty similar to the `compile:sources` tree, except that it depends on `test` scoped settings.  In some cases, you can see that the scope is `*`, this means that it's depending on an unscoped task/setting.

Configuration is not the only axis that you can scope tasks on in sbt, sbt supports two other axes, project and task.

The project axis is scoped by an sbt project.  An sbt build can have multiple projects, and each project can have its own set of settings.  When you define tasks on a project, sbt will automatically scope those tasks, and the dependencies of those tasks, to be for that project, that is if you haven't already explicitly scoped them to a project yourself.  Tasks scoped to one project can also depend on tasks in another project, so you could for example make the `packageSrc` command in one project depend on the `sources` for all the other projects, thus bringing all your sources together into one source jar.

The syntax for scoping something by project on the sbt command line is to prefix the task with the project name followed by a slash, then the task.  For example `sbt-fun/compile:sources` is the `sources` task in the `compile` scope from the `sbt-fun` project.  You can actually see from the output of the plain `inspect` command, in the *Provided By* section, that the full task is `{file:/Users/jroper/sbt-fun/}sbt-fun/compile:sources`, this is the path of the build, followed by the project name, configuration and task.  Sometimes tasks and settings are scoped to be global or for the entire build, you can see some such settings above, they are prefixed with `*/`, so `*/*:excludeFilter` is the `excludeFilter` task, with no configuration scope, and no project scope.

The final axis is to be scoped by another task.  Scoping by another task is incredibly useful, which we'll see when we get to scope fallbacks, but what it means is that the same task key can be used and explicitly configured for many tasks.  In the above tree we can see that `unmanagedSources` depends on `includeFilter` scoped to the `unmanagedSources` task, the syntax for this is `unmanagedSources::includeFilter`.  `includeFilter` may also be used elsewhere, for example, in discovering `resources`, in that case it will be scoped to the `unmanagedResources` task.

## Scope fallbacks

Scopes work in a hierarchical fashion, allowing fallbacks through the hierarchy when tasks at a specific scope can't be found.  I mentioned above that `unmanagedSources` depends on `unmanagedSources::includeFilter`.  Let's have a closer look, by inspecting it:

```
> inspect unmanagedSources
[info] Task: scala.collection.Seq[java.io.File]
[info] Description:
[info]  Unmanaged sources, which are manually created.
[info] Provided by:
[info]  {file:/Users/jroper/sbt-fun/}sbt-fun/compile:unmanagedSources
[info] Defined at:
[info]  (sbt.Defaults) Defaults.scala:182
[info]  (sbt.Defaults) Defaults.scala:209
[info] Dependencies:
[info]  compile:baseDirectory
[info]  compile:unmanagedSourceDirectories
[info]  compile:unmanagedSources::includeFilter
[info]  compile:unmanagedSources::excludeFilter
...
```

So we can see that `compile:unmanagedSources` depends on `compile:unmanagedSources::includeFilter` and `compile:unmanagedSources::excludeFilter`.  But if we have a look at the inspect tree command, we'll notice a discrepancy:

```
> inspect tree unmanagedSources
[info] compile:unmanagedSources = Task[scala.collection.Seq[java.io.File]]
[info]   +-*/*:sourcesInBase = true
[info]   +-*/*:excludeFilter = sbt.HiddenFileFilter$@5a63fa71
[info]   +-*:baseDirectory = /Users/jroper/sbt-fun
[info]   +-*/*:unmanagedSources::includeFilter = sbt.SimpleFilter@44a44a04
...
```

So, while it depended on `sbt-fun/compile:unmanagedSources::includeFilter`, it actually got `*/*:unmanagedSources::includeFilter`, that is, it requested a task at a specific project and configuration, but got a task that was defined for no project or configuration.  Furthermore, the `excludeFilter` which was similarly requested, was satisfied by `*/*:excludeFilter`, that is, it isn't even scoped to the `unmanagedSources` task.  This is a demonstration of how sbt uses fallbacks.  When a task declares a dependency, sbt will try and satisfy that dependency with the most specific task it has for it, but if no task is defined at that specific scope, it will fallback to a less specific scope.

What this means, for example for `excludeFilter`, is that if you have a text editor that generates temporary files of a particular format, you can exclude those by adding it to the global `excludeFilter`, you don't need to define an `excludeFilter` for every single scope.  But, I might also decide that I want to exclude certain files in the test scope, so I can configure a different `excludeFilter` for tests by scoping it to `test`.  Or, I might decide that I want a different filter again just for `unmanagedSources`, as opposed to `unmanagedResources`, so I can define the `excludeFilter` specifically for those tasks.  The general approach that sbt takes in its predefined task dependency trees is to depend on tasks at a very specific scope, but define them at the most general scope that makes sense, allowing tasks to be overridden in a blanket fashion, but at a very fine grained level when required.

## Parallel by default

There is one last feature of the sbt task engine that I think is worth mentioning in this post.  It's not one that really needs to be understood well in order to use sbt, but it is a very powerful one that sbt's architecture makes very simple.  In sbt, all tasks are executed in parallel by default.  Now of course, if a task declares a dependency on another task, those two tasks can't run in parallel.  But two tasks that have no dependency on each other, such as `unmanagedSources` and `managedSources`, can, and will be executed in parallel.  Given sbt's fine grained tasks, this makes for some considerable (and much needed, given the speed of scala compilation) performance improvements out of the box compared to other build tools.

sbt's concurrent execution is also configurable, tasks can be tagged, and then you can define, for example, what the maximum number of tasks with that tag can be run in parallel.  You can read more about these capabilities [here](http://www.scala-sbt.org/0.13/docs/Parallel-Execution.html).

## Conclusion

In this blog post we have seen that sbt is actually a task engine, and that the fact that it breaks tasks up into many smaller interdependent tasks gives you a lot of power and flexibility.  We have seen that the sbt console can be used to inspect tasks, their dependencies, and entire dependency graphs of tasks, and this allows us to learn about sbt, the tasks that are available, and see how our build fits together.  We have learned how tasks can be scoped to different configurations, projects, and other tasks, and how sbt uses a fallback system to resolve dependencies at specific scopes.  Hopefully sbt is now more transparent to you, you no longer need a spellbook to know how to configure it, rather, you can use the inspect commands to discover what you can configure yourself.

We have not seen anything about how to define or redefine tasks, or the syntax of the sbt build file.  This is the topic of my next blog post, [sbt - A declarative DSL](2015/03/04/sbt-declarative-dsl.html).
