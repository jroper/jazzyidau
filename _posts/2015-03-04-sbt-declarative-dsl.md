---
title: "sbt - A declarative DSL"
tags: scala sbt
---

This is my second post in a series of posts about sbt. The first post, [sbt - A task engine](2015/03/03/sbt-task-engine.html), looked at sbt's task engine, how it is self documenting, making tasks discoverable, and how scopes allow heirarchical fallbacks.  In this post we'll take a look at how the sbt task engine is declared, again taking a top down approach, rooted in practical examples.

## Settings are not settings

In the previous post, we were introduced to settings, being a specialisation of tasks that are only executed once at the start of an sbt session.  File that bit of knowledge away, and now reset your definition of setting to nothing.  I'm guessing this is a relic of past versions of sbt, but the word setting in sbt can mean two distinct things, one is a task that's executed at the start of the session, the other is a task (or setting, as in executed at start of session) declaration.  No clearer?

Let's introduce another bit of terminology, a task key.  A task key is a string and a type, the string is the name of the task, it's how you reference it on the command line.  The type is the type that the task produces.  So the `sources` task has a name of `"sources"`, and a type of `Seq[File]`.  It is defined in [`sbt.Keys`](https://github.com/sbt/sbt/blob/v0.13.7/main/src/main/scala/sbt/Keys.scala#L85):

```scala
val sources = TaskKey[Seq[File]]("sources", "All sources, both managed and unmanaged.", BTask)
```

You can see it also has a description and a rank, those are not really important to us now.  The thing that uniquely defines this task key is the `sources` string.  You could define another `sources` key elsewhere, as long as they have the same name, they will be considered the key for the same task.  Of course, if you define two tasks using the same key name, but different key types, that's not allowed, and sbt will give you an error.

In addition to `TaskKey`'s there are also `SettingKey`'s, this is setting as in only executed once per session.  Now these keys by themselves do nothing.  They only do something when you declare some behaviour for them.  So, a setting as in a task declaration is a task or setting key, potentially scoped, with some associated behaviour.  For the remainder of this post, when I say setting, that's what I'm referring to.

## Settings are executed like a program

Defining sbt's task engine is done by giving sbt a series of settings, each setting declaring a task implementation.  sbt then executes those settings in order.  Tasks can be declared multiple times by multiple settings, the last one to execute wins.

So where do these settings come from?  They come from many places.  Most obviously, they come from the build file.  But most settings don't come from there - as you're aware, sbt defines many, many fine grained tasks, but you don't have to declare settings for these yourself.  Most of the settings come from sbt plugins.  One in particular that is enabled by default is the `JvmPlugin`.  This contains all the settings necessary for building a Java or Scala program, including settings that declare the `sources` task that we saw yesterday.  Plugin settings are executed before the settings in your build file, this means that any settings you declare in your build file will override the settings declared by the plugins.

This ordering of settings is important to note, it means settings have to be read from top to bottom.  I have handled a number of support cases and mailing list questions where people haven't realised this, they have declared a setting, and then after that included a sequence of settings from elsewhere in their build that redeclares the setting.  They expected their setting to take precedence, but since their setting came before the setting from the sequence, the setting from the sequence overwrites it.

## sbt build file syntax

We're about to get into some concrete examples of declaring settings, so before we do that we better cover the basics of the sbt build file.  sbt builds can either be specified in plain `*.scala` files, or in sbts own `*.sbt` file format.  As of sbt 0.13.7, the sbt format has become powerful enough that there is really not much that you can't do with it, so we're only going to look at that.

An sbt file may have any name, convention is that a projects main build file be called `build.sbt`, but that is only a convention.  The file may contain a series of Scala statements and expressions, and it's important here to distinguish between statements and expressions.  What's the difference?  A statement doesn't return a value.  For example:

```scala
val foo = "bar"
```

This is a statement, it has assigned the val `foo` to `"bar"`, but this assignment doesn't return a value.  In contrast:

```scala
5 + 4
```

This is an expression, it returns a value of type `Int`.

Expressions in an sbt file must have a type of either `Setting[_]` or `Seq[Setting[_]]`.  sbt will evaluate all these exrpessions, and add them to the settings for your build.  Any expression in your sbt file that isn't of one of those types will generate an error.

Statements can be anything.  They can be utility methods, vals, lazy vals, whatever.  In most cases, sbt ignores them, but that doesn't make them useless, you can use them in other expressions or statements, to help you define your build.  There is one type of statement though that sbt doesn't ignore, and that is statements that assign a val to a project, this is how projects are defined:

```scala
lazy val sbtFunProject = project in file(".")
```

The final thing to know about sbt build files is that sbt automatically brings in a number of imports.  For example, `sbt._` is imported, as is `sbt.Keys._`, so you have access to the full range of built in task keys that sbt defines without having to import them yourself.  sbt also brings in imports declared by plugins, making it straight forward to use those plugins.

## Declaring a setting

The process of declaring a setting is done by taking a task key, optionally apply a scope to it, and then declaring an implementation for that task.  Here's a very basic example:

```scala
name := "sbt-fun"
```

In this case we're declaring the implementation of the `name` task to simply be a static value of `"sbt-fun"`.  Note that the above is a expression, not a statement.  `:=` is not a Scala language feature, it is actually a method that sbt has provided.  sbt's syntax for declaring settings is a pure scala DSL.  If this syntax confuses you, then I strongly recommend that you read a post I wrote a few years ago called [Three things I had to learn about Scala before it made sense](2012/03/19/three_things_i_had_to_learn_about_scala_before_it_made_sense.html).  This post explains how DSL's are implemented in Scala, and is essential reading before you read on in this post if you don't understand that already.

What if we want to declare our own implementation of the `sources` task?  Remembering that we want it scoped to `compile`, we can do this:

```scala
sources in Compile := Seq(file("src/main/scala/MySource.scala"))
```

Again we're only setting a static value for this task to return, but this time you can see how we've scoped the `sources` task in the `compile` scope.  Note that configurations such as `compile` and `test` are available through capitalised vals, in scope in your build.

## Back to first principles

What if we want to declare a dependency on another task?  Let's say we want to declare `sources` to be, as it's described, all managed and unmanaged sources.  If you've used sbt before, you probably know that you can use this syntax:

```scala
sources := managedSources.value ++ unmanagedSources.value
```

This was introduced is sbt 0.13, and it's actually implemented by a macro that does some magic for you.  It's great, I use that syntax all the time, and so should you.  However, as with anything that does magic for you, if you don't understand what it's doing for you and how it does it, you can run into troubles.

As I described in the last post, sbt is a task engine, and tasks declare dependencies that are executed before, and provided as input, to them.  In the above example, it doesn't look like this is happening at all, what it looks like is that when the `sources` task is executed, it executes the `managedSources` task by calling `value`, and the `unmanagedSources` task by calling `value`, and then concatenates their results together.  There is a macro that is transforming this code to something that does declare dependencies, and takes the inputs of those dependencies and passes them to the implementation.

So in order to understand what the macro is doing for us, let's implement this ourselves manually - let's declarce this setting from first principles.

Firstly, we're going to use the `<<=` operator instead, this is how to say that I am declaring this task to be dependent on other tasks.  Now, we could do a very straight forward declaration to another task:

```scala
sources <<= unmanagedSources
```

This will say that the `sources` task has a dependency on `unmanagadSources`, and will take the output of `unmanagedSources` as is, and return it as the output of `sources`.  What if we wanted to change that value before returning it?  We can do that using the `map` method:

```scala
sources <<= unmanagedSources.map(files => files.filterNot(_.getName.startsWith("_")))
```

So now we've filtered out all the files that start with `_` (note that sbt already provides an `excludesFilter` task that can be used to configure this, this is just an example).

At this point let's take a step back and think about what the code above has done.  For one, nothing has yet been executed, at least not the task implementation.  That `<<=` method returns an object of type `Setting`.  This setting has the following attributes:

* The key (potentially scoped) that it is the task declaration for, in this case, `sources`.
* The keys (potentially scoped) of tasks that it depends on, in this case, `unmanagedSources`.
* A function that takes the output of the tasks that it depends as input parameters, executes the task, and returns the output of the task being declared (that is, the function we passed to the `map` method, that filters out all files that start with `_`).

You can see here that we haven't actually executed anything in the task, we have only declared how the task is implemented.  So when sbt goes to execute the sources task, it will find this declaration, execute the dependencies, and then execute the callback.  This is why I've called this blog post "sbt - A declarative DSL".  All our settings just declare how tasks are implemented, they don't actually execute anything.

So, what if we want to depend on two different tasks?  Through the magic of the sbt DSL, we can put them in a tuple, and then map the tuple:

```scala
sources <<= (unmanagedSources, managedSources).map { (unmanaged, managed) => unmanaged ++ managed) }
```

And now we actually have our first principles implementation of the `sources` task.  Sort of, we haven't scoped it to `compile`, but that's not hard to do:

```scala
sources in Compile <<= (unmanagedSources in Compile, managedSources in Compile).map(_ ++ _)
```

For brevity I've used a shorter syntax for concatenating the two sources sequences.

## sbt uses macros heavily

So now that we've seen how to declare tasks from from first principles, let's see how the macros work.  We have our declaration from before:

```scala
sources := { managedSources.value ++ unmanagedSources.value }
```

I've inserted the curly braces to make it clear what is being passed to the `:=` macro.  The `:=` macro will go through the block of code passed to it, and find all the instances of where `value` is called, and gather all the keys that it is invoked on.  It will then generate Scala code (or rather AST) that builds those keys as a tuple, and then invokes map.  To the map call, it will pass the original code block, but replacing all the keys that had value on them with parameters that are taken as the input arguments to the function passed to map.  Essentially, it builds exactly the same code that we implemented in our first principles implementation.

Now, it's important to understand how these macros work, because when you try to use the `value` call outside of the context of a macro, you will obviously run into problems.  An important thing to realise is that the code generated by the macro never actually invokes `value`, `value` is just a place holder method used to tell the macro to extract these keys out to be dependencies that get mapped.  The `value` method itself is in fact a macro, one that if you invoke it outside of the context of another macro, will result in a compile time error, the exact error message being `value can only be used within a task or setting macro, such as :=, +=, ++=, Def.task, or Def.setting.`.  And you can see why, since sbt settings are entirely declarative, you can't access the value of a task from the key, it doesn't make sense to do that.

From now on in this post we'll switch to using the macros, but remember what these macros compile to.

## Redeclaring tasks

So far we've seen how to completely overwrite a task. What if you don't want to ovewrite the task, you just want to modify its output?  sbt allows you to make a task depend on itself, if you do that, the task will depend on the old implementation of itself, giving the output of that implementation to you as your input.  In the previous blog post, I brought up the possibility of only compiling source files with a certain annotation inside them, let's say we're only going to compile source files that contain the text `"COMPILE ME"`.  Here's how you might implement that, depending on the existing `sources` implementation:

```scala
sources := {
  sources.value.filter { sourceFile =>
    IO.read(sourceFile).contains("COMPILE ME")
  }
}
```

sbt also provides a short hand for doing this, the `~=` operator, which takes a function that takes the old value and returns the new value:

```scala
sources ~= _.filter { sourceFile =>
  IO.read(sourceFile).contains("COMPILE ME")
}

```

Another convenient shorthand for modifying the old value of a task that sbt provides, and that you have likely come across before, is the `+=` and `++=` operators.  These take the old value, and append the item or sequence of items produced by your new implementation to it.  So, to add a file to the sources:

```scala
sources += file("src/other/scala/Other.scala")
```

Or to add multiple files:

```scala
sources ++= Seq(
  file("src/other/scala/Other.scala"),
  file("src/other/scala/OtherOther.scala")
)
```

These of course can depend on other tasks through the `value` macro, just like when you use `:=`:

```scala
sources ++= Seq(
  (sourceDirectory.value / "other" / "scala").***
)
```

The `***` method loads every file from a directory recursively.

## Scope me up

We've talked a little bit about scopes, but most of our examples so far have excluded them for brevity.  So let's take a look at how to scope settings and their dependencies.

To apply a scope to a setting, you can use the `in` method:

```scala
sources in Compile += file("src/other/scala/Other.scala")
```

Applying multiple scopes can be done by using multiple `in` calls, for example:

```scala
excludeFilter in sbtFunProject in unmanagedSources in Compile := "_*"
```

Or, they can also be done by passing multiple scopes to the `in` method, in the order project, configuration then task:

```scala
excludeFilter in (sbtFunProject, Compile, unmanagedSources) := "_*"
```

The same syntax can be used when depending on settings, though make sure you put parenthesis around the whole scoped setting in order to invoke the `value` method on it:

```scala
(sources in Compile) := 
  (managedSources in Compile).value ++ 
  (unmanagedSources in Compile).value
```

## Conclusion

In the first post in this series, we were introduced to the concepts behind sbt and its task engine, and how to explore and discover the task graphs that sbt provides.  In this post we saw the practical side of how task dependencies and implementations are declared, using both the map method to map dependency keys, as well as macros.  We saw how to modify existing task declarations, as well as how to use scopes.

One thing I've avoided here is showing cookbooks of how to do specific tasks, for example, how to add a source generator.  The [sbt documentation](http://www.scala-sbt.org/0.13/docs/index.html) is really not bad for this, especially for cookbook type documentation, but I also hope that after reading these posts, you aren't as dependent on copying and pasting sbt configuration, but rather can use the tools built in to sbt to discover the structure of your build, and modify it accordingly.
