---
title: "CQRS increases consistency"
tags: cqrs
---

> The problem with CQRS is that it makes things more complex because it decreases consistency.

I hear this argument a lot. As the author of a [framework that strongly encourages CQRS](http://www.lagomframework.com), I obviously am biased against this opinion, and disgaree with it.

Of course, the statement and my disagreement needs context and qualification. If you have a traditional monolithic system, where all data operations are done on a single database, and that single database supports ACID transactions, then yes, switching to CQRS will decrease consistency. However, that's not the context in which CQRS usually comes up, and it's not the context that I recommend using it. The context is microservices, and more and more industry experts are recommending that people move away from monoliths and move to microservices.

## Microservices decrease consistency

A core principle of microservices is that they should own all their own data. Another core principle is that they should be able to act autonomously. This means they shouldn't need to depend on other services - in particular making synchrconous calls to other services - in order to implement the functions they are responsible for.

If a service is to be autonomous then, it needs to store all the data necessary for it to achieve its functions. Sometimes this data may be owned by other services - and so in order to function, those services need to publish that data for this service to consume and update its own store. This means in a microservices system, the same data is going to be stored in many places. Will this data be strongly consistent across the entire system? Unless every time you do an operation, you stop all other requests to the entire system, and process the operation until it succeeds, the answer is no, it will often be inconsistent.

So, a switch to microservices is by nature a switch to decreased consistency. Decreased consistency is a consequence of microservices.

## Inconsistency can be very bad

Because we are using microservices, our system is inconsistent, and we need to deal with that. How inconsistent our system can get depends on how we deal with it. The typical, very high level hand wavy approach to consistency is to use eventual consistency. But how is that actually achieved?

Let's imagine service B needs some data owned by service A to implement its responsibility. In this scenario, A is responsible for writing the data, it's the thing that handles the command, while B is responsible for reading the data, it does the query. In order for B to act autonomously though, it can't query A directly, it needs to have A push it the data so that it can update its own query store, and then when the time comes to use it, query it locally.

Service A could do this synchronously, when an operation is done on service A, it will make a call on service B to update it with the results. Now this works fine on paper, but what happens when service B is down at that time? Does service A have to keep retrying until service B comes back up? What if service A then goes down?

As you can see we now have a consistency problem, one that will not eventually become consistent. Service A has been updated, but service B failed to update. It will stay inconsistent forever, we could say that it is terminally inconsistent, and will require manual intervention in order to fix. The reason we have this problem is that we combined our command responsibility and query responsibilities in the one operation.

## CQRS gives you consistency back

CQRS means separating command responsibilities from query responsibilities. In our scenario, using CQRS, service A handles the command, and updates its state, and is done. Then, in a separate operation, another process will take the result of the operation on A, and asynchronously push it to service B.

Since the operation is asynchronous, service B doesn't need to up at the time - for example it can use an at least once messaging queue to handle the message. If service B isn't up at the time, then the system will be inconsistent for a period of time. However, when service B comes back up, and then processes the message, the service will become consistent again, it will be eventually consistent.

So by employing CQRS, we are able to get back some of the consistency guarantees that we lost when we moved to microservices. We don't have a globally consistent system, but we can guarantee an eventually consistent system.

## CQRS is a necessary evil

CQRS is complex, far more complex than handling both the command and query responsibilities of data in single operations on a strongly consistent database. However, in a microservices world, we don't have the luxury of relying on a single strongly consistent database. In that world, inconsistency is a given. If someone says using CQRS in microservices means you lose consistency - they have failed to acknowledge that they lost consistency the moment they started using microservices, it was not CQRS that lost them that consistency. Rather, CQRS is a very powerful tool that allows us to address the inherent inconsistency of microservices to give us eventual consistency instead of terminal inconsistency.

