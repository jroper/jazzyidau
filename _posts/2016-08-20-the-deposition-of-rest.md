---
title: "The deposition of REST"
tags: rest
---

When I first sat down to write this blog post, I was going to call it "The death of REST". Had I have done that I would immediately alienated many readers, and also overstepped the message that I'm trying to convey. REST is not dead, nor will it die. Nor is it not useful. But at the same time, I do want to pick a fight against the use of REST. After thinking about it for a while, I've realised that it's not the death of REST that I'm calling for, it's the deposition of it.

If REST is to be deposed, this implies that REST currently has a seat at the top of a hierarchy, and it certainly does. REST is currently the default architectural style for application level communication used by many in our industry, particularly in enterprise systems. It's what we think of first when we need to answer the question of how do I get information from one service to another. In this context I'm referring specifically to inter-service communication within a single system, and it's this top place in our selection of archetictural styles for inter-service communication that I believe REST should be deposed from.

That doesn't mean that there aren't still many good use cases for REST. The continued and growing success of the web can be largely attributed to REST - its stateless nature and uniform interface are two of the most useful properties of the REST archetictural style that has allowed the web to flourish not just as a web of interconnected pages, but as a web of interconnected applications, sharing data and communicating with each other with relative ease. Certainly, for the web itself, REST remains and likely will always be the default go to architectural style, and there's nothing wrong with that.

Beyond the web, there also are still places where REST is appropriate, including in inter-service communication. As I asserted earlier, REST is not dead or dying. It simply needs to be deposed from being the default go to architectural style that we use when we write our systems. REST should be better thought of as a tool that may be used when the use case is right, after first considering the use of other, more broadly appropriate architectural styles.

I should also point out that (for once) when I say REST I'm not referring to HTTP. The REST architectural style is practically always implemented using HTTP, to the point where the industry at large considers them synonymous. However, there are other architectural styles that can be implemented in HTTP that are not REST. Just as REST is bigger than its implementation in HTTP, HTTP is also bigger than its application to REST, and even with REST deposed, HTTP may still play an important part in the implementation of inter-service communication. With this in mind, the alternatives to REST that I discuss can definitely be implemented in HTTP, even in a way that *looks* like REST, but these alternatives are conceptually different at a fundamental level to REST, they are a different architectural style requiring a different approach to the way we think about systems architecture.

## Why should REST be deposed?

The REST architectural style was first defined by Roy Fielding in his PhD dissertation [Architectural Styles and Design of Network-based Software Architectures](https://www.ics.uci.edu/~fielding/pubs/dissertation/top.htm) in 2000. To this day, this dissertation remains the most authoratative paper on REST, and is frequently read and cited by many in the industry.

In his derivation of REST, Fielding outlines six constraints that form the basis for the REST architectural style: Client-Server, Stateless, Cache, Uniform Interface, Layered System, and Code-On-Demand. Some of these constraints are great and I don't have any argument against them. Some of them are dependent on the others, and don't have any meaningful application if the other constraints don't apply. However there are two constraints in particular that present problems for modern systems, and these I will address below.

### Client-Server

The principle that drives client server constraints is separation of concerns. This principle is incredibly important. It means that a client does not have to worry about the implementation of a service.

At the time that Fielding wrote his dissertation, services behaved like a single machine. In practice they were usually more than one machine, at a minimum they tended to consist of an application server and a database server, sometimes the application servers and database servers consisted of multiple nodes. But, from the outside, these groups of physical machines behaved as one logical, consistent, single machine. This meant that the client server architectural pattern could adequately allow a client not to worry about the implementation of a service.

Today's world is different. Services no longer behave like a single machine. In microservice architectures, what used to be one service is now many services, each with their own consistency boundaries. Even within a single microservice, the scaling demands of todays world of billions of always connected devices are such that that service may be made up of thousands of machines. Within the service there may be many consistency boundaries, between entities, between views, and so the service can no longer present the facade of a single machine.

The consequence of this is that the client server architectural constraint is no longer able to achieve the principle of separation of concerns. Clients must be aware of the servers consistency boundaries, which resources may return inconsistent results, and be prepared to handle this in order to successfully interact with the server. It means the client may be responsible for detecting inconsistencies in the service, and retrying later if necessary. Consistency boundaries are and should be an implementation detail of the server, and so leaking it out to the client violates the principle of separation of concerns.

Another leakage that arises in the modern world is a servers availability. In a large distributed system, failure is not an exceptional circumstance, it is an every day situation. Services may partially fail, and their failure and recovery from failure in a well designed system should be an implementation detail that clients do not have to concern themselves with. Instead, in the client server model, clients are forced to explicitly handle this failure, for example, using circuit breakers, or retrying at a later time. This leakage of failure and recovery to clients is another violation of separation of concerns.

### Uniform Interface - Resources

Fielding's uniform interface constraint is possibly the most well known and best associated constraint with REST. It was the constraint that most significantly contrasted REST with other architectural styles at the time that Fielding wrote his dissertation. The constraint itself has a number of parts, and some of these sub constraints address important principles that few if any other architectural styles have been able to address as well as REST has addressed.

The guiding principle behind each of the parts of the uniform interface constraint is once again a very important principle, that is the principle of generality. As stated by Fielding, this principle simplifies system architecture and improves the visibility of interactions.

The first part of the uniform interface constraint is the representation of information as a resource. Fielding defines a resource as follows:

> a resource *R* is a temporally varying membership function *M<sub>R</sub>(t)*, which for time *t* maps to a set of entities, or values, which are equivalent.

The problem with this constraint is its use of time. Fielding doesn't explicitly define time here, but he seems to implicitly correlate time with present, that is to say, the *t* that gets input into the above function is whatever the present time is. This presents a major challenge to distributed systems, since distributed systems are unable to model present. When a system consists of many services with their own consistency boundaries, each service consisting of many machines with their own consistency boundaries, how can they agree on what the present state is? There is no global present in a distributed system.

In practice, this has a few implications. One implication is that the function may return two different answers for the same *t* - that is, a system, or a service in the system, may return inconsistent answers. For example, a client may retrieve a resource, and then attempt to update that resource, only to find out that that resource doesn't exist - not because it's been deleted, but because the machine handling the update doesn't know about the resource yet. Consider a blog system, if I post a blog post, and then someone views that blog post and comments, but there was a delay (maybe due to failure) in the knowledge of the blog post being propogated to the service that handles comments, the comment service may respond saying that the blog post doesn't exist and so I can't comment on it. In this situation, at the same *t*, the system both says the resource exists, and that it doesn't exist.

Another problem exhibits in failure handling. Distributed systems frequently fail, to the extent that failure must be treated not as an exceptional circumstance, but a normal circumstance. The simplest way to deal with failure is to retry, however, when treating resources as a function on present time, retrying is not safe. Why not? Because *t* has changed. You can't go back in time to get the old value of *t* to replay the resource membership function. The assumptions made when the action was first executed and failed may no longer hold.

So although the principle of generality behind the constraint is solid, the constraint itself is often not implementable in modern systems, leading to complex failure and inconsistency scenarios that are difficult to recover from.

## What should depose REST?

We have seen how two of the fundamental constraints behind REST cause problems in modern systems. If we are going to depose REST, we must first come up with alternative constraints that can be implemented in modern systems that uphold the principles that REST is derived from. 

### Publish-Subscribe

Where the client server constraint hid implementation details of the service from the client, publish subscribe also hides implementation details of the service from the client. However it does so in a way that does not require the server to behave as one machine. Publish subscribe allows a service to manage its own consistency boundaries without exposing them to the client. A service can, for example, delay publishing messages until it is sure that they will be consistent. A service may also split its consistency boundaries up, such that within one boundary, messages can be safely published and present a consistent view of the world.

Clients also don't need to take into consideration the services availability. If the service fails, or is overloaded, the service itself is responsible for recovering from that. It can implement its own failure handling and recovery independent to the client, it can change these implementations independently to the the client, and the client doesn't even need to know that the service was ever unavailable.

### Uniform Interface - Facts

The principle of generality which guides a uniform interface constraint is key to the simplicity of the overall architecture of a system, but as we saw before the reliance on present when information is represented as resources poses a major problem to modern distributed systems. This principle can be maintained if, instead of representing information as resources, we represent information as facts.

Facts are events that happened at points in time in the past. They do not change over time, they are immutable, they are indisputable, and they are intrinsically consistent. Facts can safely be passed between services in distributed systems without worrying about consistency, since regardless of what the present state is, a fact remains true. If a machine in a system wants to maintain a present, it can do that by subscribing to the facts, and deriving the present from that. Its present may be different from other machines' present, because they may have seen different facts, some facts may not have propogated to every machine yet. But as long as that present is kept internal, and not published, that's ok.

Services can keep track of the facts they know about. In doing so, they can implement idempotent handling of facts, which means facts can safely be replayed in the event of failure. This allows the use of easy to implement, scale and understand patterns such as at least once message delivery, something that could cause problems in a model that architected itself around verbs to maniuplate resources.

In representing information as facts, we still have implemented the principle of generality, which keeps our architecture simple, but we have done so in a way that can be safely implemented in a distributed system.

### The rest of REST

We have seen how two problems in the constraints that REST is derived from can be addressed with alternative constraints. If an alternative architectural style however is to depose REST, that alternative must also be able to address the remaining principles that guide the constraints of REST. I will briefly address these constraints here:

* The stateless constraint is implied by publish subscribe, since not only do servers not hold state about a client between requests, servers have no knowledge of clients whatsoever, and so remain completely stateless with respect to clients. In fact, publish subscribe better implements the stateless constraint, as it means communication is not only stateless to the server, it's stateless to the client. As we saw, the client does not need to know of the availability state of the server in order to communicate with it.
* The cache constraint is implied and implemented in the way clients handle the facts they receive. Typically a client will derive a present state that it is interested in from the facts, and hold that state itself. No external caching is required, the state is cached by the client.
* The rest of the parts of the uniform interface constraint are not as relevant - facts are pushed, they don't need to be addressed, since there is no operation that you can perform on an immutable fact. And since facts are immutable, there is no manipulation to be done. Facts can be self descriptive, so the self descriptive constraint can still be applied, though this is possibly one feature of REST that has not been well implemented outside of it, especially in a publish subscribe architecture. Finally, the stream of published facts itself is the engine of application state, no hypermedia is needed.
* A system that publishes and subscribes to facts can and should be layerable - in particular, there may be layers to ensure at least once messaging, layers to broadcast or shard facts, layers that transform and derive new facts, and so on.
* The code on demand constraint is an optional constraint that I have not seen implemented outside of the web, for this reason I believe it's not relevant in an inter-service context.

### The deposer - Reactive

With these new constraints in mind, we now have an architectural style that can depose REST. This architectural style is actually not new at all, it is a reactive architectural style. The [Reactive Manifesto](http://www.reactivemanifesto.org), at its base, is about systems reacting to events via message passing, which forms the foundation of our publishing and subscribing of facts.

## Conclusion

We have seen how REST is insufficient as the default architectural style for modern distributed systems. To address this, we have derived some new constraints, in particular replacing client server with publish subscribe, and replacing resources with facts. These constraints are part of the reactive archictural style, and I believe they should depose REST from its place as the go to architectural style.

Once again I would like to reiterate that I am not talking about the death of REST. There will still be places in every system where REST is the appropriate architectural style to use. However, it should not be the default. I am advocating that, when information needs to be transfered between services in a system, the first thing an architect should consider is whether that information can be transfered by publishing and subscribing to facts. If not, then other models, of which the REST architectural style is one of, may be considered.

## Final notes

None of the ideas presented in this blog post are novel. In fact, publish subscribe and representing information using facts predate even REST. Perhaps my presentation of these ideas in direct contrast to the derivative constraints of REST is new, though it would not surprise me if someone has done this before. In preparing for this blog post, I have read and drawn heavily from a number of sources, three in particular are:

* [Life Beyond Distributed Transactions: an Apostates Opinion](http://www.ics.uci.edu/~cs223/papers/cidr07p15.pdf) by Pat Helland
* [Life Beyond the Illusion of Present](https://www.youtube.com/watch?v=Nhz5jMXS8gE) by Jonas Bonér
* [The Reactive Manifesto](http://www.reactivemanifesto.org/)

There are many places in this post where I have made unsupported arguments and assumed acceptance of several principles of distributed systems that many people reading this may not have heard before. This is not a dissertation or paper of any sort, it is a blog post, and so I have not gone into detail on arguments that support some of my claims. I would therefore recommend reading/watching the above resources to get a better understanding of the topic of distributed systems.
