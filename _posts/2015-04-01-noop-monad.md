---
title: "The Noop Monad - doing nothing safely"
tags: scala monad
---

If you're a fan of functional programming, as I am, you'll know that one of the great things about it is how useful it is.  But that isn't the only great thing about functional programming, functional programming is also great for when you want to do nothing at all.  Some might even say that doing nothing at all is where functional programming really shines.

So today I'm going to introduce a monad that surprisingly isn't talked about a lot - the noop monad.  The noop monad does nothing at all, but unlike noops in other programming paradigms, the noop monad does nothing safely.

## A demo

For this demonstration, I'm going to use Scala, with Scalaz to implement the monad.  Let's start off with the `Noop` type:

@[nooptrait](/_code/noop-monad/NoopMonad.scala)

As you can see, the `Noop` type has a type parameter, so we can do nothing of various types.  We can also see the `run` function, and it returns `Unit`.  Now typically in functional programming, returning `Unit` is considered a bad thing, because `Unit` is not a value, so any pure function that returns `Unit` must have done nothing.  But since `Noop` actually does do nothing, this is the one exception to that rule.  So the `run` function can be evaluated to do the nothing of the type that this particular `Noop` does.

Now, let's say I have method that calculates all the primes up to a given number.  Here's its signature:

@[calculateprimes](/_code/noop-monad/NoopMonad.scala)

And let's say I want to get a list of all the `Int` primes, I can use the above method like so:

@[allintprimes](/_code/noop-monad/NoopMonad.scala)

But wait, you say! That code is going to be very expensive to run, it's likely to take a very, very long time, and you have better things to do.  So, you want to ensure that the code doesn't run.  This is where the noop monad comes on the scene, using the `point` method, you can ensure that it safely doesn't run:

@[noopprimes](/_code/noop-monad/NoopMonad.scala)

And then, when you actually don't want to run it, you can do that by evaluating the `run` function:

@[runnoopprimes](/_code/noop-monad/NoopMonad.scala)

For those unfamiliar with scalaz and functional programming, a monad is an applicative, and an applicative is something that lets you create an instance of the applicative from a value.  The method on `Applicative` for doing this is called `point`, in other languages it's also called `pure`.

So, we can see that `Noop` is an applicative, but can we `flatMap` it?  What if you don't want to sum all those prime numbers, and then you certainly don't want to convert that result to a `String`?  The noop monad lets you do that:

@[summed](/_code/noop-monad/NoopMonad.scala)

And so then to ensure that we don't actually do all this expensive computation, we can run it as before:

@[runsummed](/_code/noop-monad/NoopMonad.scala)

## Advantages

We can see how the noop monad can be used to do nothing, but what are the advantages of using the noop monad compared to some other methods of doing nothing?  I'm going to highlight three advantages that I think really demonstrate the value of doing nothing in a monadic way.

### Runtime optimisation

This is often an advantage of functional programming in general, but the noop monad is the exemplar of optimization in functional programming.  Let's have a look at the implementation of the noop monads `point` method:

@[point](/_code/noop-monad/NoopMonad.scala)

Here we can see that not only is the passed in value not evaluated, it's not even referenced in the returned `Noop`.  But how can the noop monad do this?  Since the noop monad knows that you don't want to do anything at all, it is able to infer that therefore it will not need to evaluate the value, and therefore it doesn't need to hold a reference to the passed in value.  But this advanced optimisation doesn't stop there, let's have a look at the implementation of `bind`:

@[bind](/_code/noop-monad/NoopMonad.scala)

Here we can see a double optimisation.  First, the passed in `Noop` is not referenced.  The noop monad can do this because it infers that since you don't want to do anything, you don't need the nothing that you passed in.  Secondly, the passed in bind function is never evaluated.  As with the other parameter, the noop monad can infer that since the passed in `Noop` does nothing, there will be nothing to pass to the passed in function, and therefore, the function will never be evaluated.

As you can see, particularly for performance minded developers, the noop monad is incredibly powerful in its ability to optimise your code at runtime to do as little of nothing as possible.

### Code optimisation

But performance isn't the only place that the noop monad can help with optimisation, the noop monad can also help at optimising your code to ensure it is as simple and concise as possible.

Let's take our previous example of summing primes:

@[optimise1](/_code/noop-monad/NoopMonad.scala)

Now, this isn't bad looking code, but it does feel a little too complex when all we wanted to do in the first place was nothing.  So how can we simplify it?  Well firstly, you'll notice that we don't want to convert the summed result to a string, you can tell this by the `.point[Noop]` after it.  Based on the rules of the noop monad, we can optimise our code to remove this:

@[optimise2](/_code/noop-monad/NoopMonad.scala)

Is this safe to do?  In fact it is, because we have actually replaced our intention of doing nothing, with nothing.  We can do the same for summing all the primes:

@[optimise3](/_code/noop-monad/NoopMonad.scala)

Now the final step in code optimisation, and this is the hardest to follow so bear with me, we can actually remove the not calculating the primes itself, and simultaneously remove the `run` function on that `Noop`.  But how is this so?  You may remember that I explained earlier if a pure function returns `Unit`, then it must do nothing.  Our `Noop.run` is a pure function, and it does nothing.  So since evaluating `run` does nothing, we can safely replace it with nothing.  Finding it hard to follow?  This is what it looks like in code:

<pre class="prettyprint"><code class="language-scala">

</code></pre>

As you can see, we've gone from five reasonably complex lines of code, to absolutely no code at all!  This is the embodiment of what Dijkstra meant when he said:

> If we wish to count lines of code, we should not regard them as "lines produced" but as "lines spent".

The noop monad has allowed us to spend zero lines of code in doing nothing.

### Teaching monads

Teaching monads has proven to be the unicorn of evangelising functional programming, no matter how hard anyone tries, no one seems to be able to teach them to a newcomer.  The noop monad solves this by grounding monads in a context that all students can relate to - doing nothing.

In particular, the noop monad does a great job for picking up the pieces of a failed attempt to teach a student monads.  For example, consider the following situations:

* A student has been told that [monads are just monoids in the category of endofunctors](http://james-iry.blogspot.com.au/2009/05/brief-incomplete-and-mostly-wrong.html).  What does that even mean?  But if I say the noop monoid in the category of endofunctors is just something that does nothing, simple!
* A student has been told that [monads are burritos](http://blog.plover.com/prog/burritos.html).  What does that even mean?  But if I say the noop burrito is just something that does nothing, simple!

## Conclusion

So today I've introduced you to the noop monad.  As you can see, it's in the noop monad that functional programming is made complete, fullfilling everything that every functional programmer has ever wanted to do, that is, nothing at all.

