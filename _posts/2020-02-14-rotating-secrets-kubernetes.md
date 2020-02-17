---
title: "Rotating secrets in Kubernetes"
tags: kubernetes
---

I'm building a multitenant SaaS on top of Kubernetes at the moment, and one principle we've gone with is that all secrets should be rotated regularly. I'm surprised by the distinct lack of documentation on best practices for how to supply this configuration in Kubernetes.

Of course, when it comes to certificates, it's fairly straight forward to rotate a certificate using cert-manager, but even that isn't quite solved - while you can rotate a services certificate, there's no straight forward way to rotate the certificate for the CA that it and the other services it authenticates with trusts.

When it comes to authentication that is not based on certificates, such as symmetric encryption keys, passwords, or assymetric keys used for things like JWTs, there is nothing really out there saying how to do this.

Of course, it's absolutely possible to do it, and I can come up with multiple different ways in my head to do this without requiring downtime. There are also multiple third party solutions, such as HashiCorp Vault, that makes it possible. But I'd like a Kubernetes native mechanism - afterall, Kubernetes does provide a secret management API, it should be possible to use this in a way that supports secret rotation.

So, I'm going to propose a convention, that perhaps could become a best practice, for how to manage secrets in Kubernetes in a way that is compatible with secret rotation. What I'm proposing is all manual, but it shouldn't be too hard to build tooling around this approach to make it automatic.

## A few principles

Firstly, for secret rotation to work, I need to outline a few principles.

### Secrets should be read, and reread, from the filesystem

One of the great things about Kubernetes secrets is that when mounted as filesystem volumes, an update to the secret is immediately available to the pods consuming it. No restart or redeploy is needed, all you have to do is update the secret. However, to take advantage of this, the code consuming the secret must read it from the filesystem. It doesn't work for secrets passed using environment variables. Additionally, the code must, at least periodically, reread the secret from the filesystem, otherwise it won't pick up the changes.

We've had success in Akka configuring this for encryption keys and certificates. When we read the secret, we track a timestamp of when we read it. When we next access the certificate (next time we receive or make a new TLS connection), if the timestamp is more than 5 minutes old, we reread it. This way, we can rotate secrets with zero interruption to the service.

### Secrets must be identified by an id

When it comes to TLS certificates, the id of the secret is built in to the certificate, and TLS implementations can typically be configured with multiple trusted certificates to use. For JWTs, there is a semi built in mechanism, you set the key id in the key id JWT parameter. However, most JWT implementations don't make you set this by default, and sometimes provide only limited if any support for dynamically selecting a key to use based on the passed in key id.

When encrypting arbitrary data, there's not usually any built in mechanism to indicate the id of the key used. In my case, when we encrypt small amounts of data, we are encoding using the format `<keyid>:<base64ed initialization vector>:<base64ed cypher text>`. This allows us to associate each piece of encrypted data with the key that was used to encrypt it.

For passwords, it gets harder again, because generally, only one password can work at a time. One possibility here is to support multiple usernames, and use the username as the key id.

## Rotation mechanism

Having set out our principles by which the code that consumes our secrets will abide by, we can now propose a mechanism for configuring and rotating secrets in Kubernetes.

Kubernetes secrets allow multiple key value pairs. We can utilise this. When these secrets get mounted as volumes, the filename corresponds to the key in the key value pair. Given the name clash between secret keys, and key value keys, I'm going to refer to the key value keys as filenames.

Consider the case where you might have a symmetric key, perhaps used for signing JWTs. Each key will get an identifier, this could simply be a counter, a timestamp, a UUID, etc. When there is only one key in use, the filename might be `<key-id>.key`. When your code loads the key, it looks in the directory of the volume mount for any files called `*.key`, and will load them all, storing them in a map of key ids to the actual key contents.

Now, this works great when validating JWTs, since you have a key id before you start, and you just need to select the secret for that key id. However when creating a JWT, if you have multiple keys configured, which one do you use? It's important that you choose the right one, if the secret has only just been updated to add a second key, other pods may not yet have picked up that second key, and so if you use that second key to sign a JWT and send it to those pods, they may fail to validate it. So, to handle this, we will also support specifying a primary secret, by naming it `<key-id>.key.primary`. There must only be one primary secret, and it will always be the one used to sign or encrypt data.

So, given this set up, this will be our rotation mechanism:

* A given secret starts with a single key configured, let's say its id is `r1`. The id can be anything, we're using `r` to stand for revision, and `1` to indicate its the first key, but the id could be a UUID or anything else. The key will be placed in a Kubernetes secret, with a filename of `r1.key.primary`.
* When it comes time to rotate the key, a new key will be generated, and a new id assigned, `r2`. The kubernetes secret will be updated so that it now has both keys, with `r1.key.primary` being the filename for the old key, and `r2.key` being the filename for the second key.
* Once we are sure that all nodes have picked up the new key, we can now change the new key to be the primary. So, the secret is again updated, with `r1.key` being the filename for the old key, and `r2.key.primary` being the filename for the new key.
* After some time, eg, once we are sure that all JWTs signed by the old key have expired, we will delete the old key, updating the secret so it only contains one key, with a filename of `r2.key.primary`.

This approach can also work when a secret comes in multiple parts, such as assymetric keys, or self signed certificates, simply replace `key` with the name of the thing, so for example, I might have `r2.private` and `r2.public` or `r2.crt`.

## Why bother?

It's not too hard for someone to implement the above themselves, but why bother proposing it as a best practice? If the above approach was to be adopted by many different people, this would open the following possibilities:

### Secret consumption support

Secret consumers could provide built in support for this convention. For example, a JWT implementation might offer it, users would just need to pass it the directory to find the keys, and it would use them, periodically rereading and consuming the new keys. HTTP servers and clients could do likewise, as could database drivers. Generic libraries that implement the convention could be provided so that arbitrary secret usage, such as for encryption, could easily consume the keys.

### Automatic rotation

If enough consumers were using the convention, tools for automatic secret rotation could be implemented. This could be as simple as an operator that would allow you to configure a secret to be generated and rotated, given paramaters such as how frequently to rotate secrets, how long to wait before making the new secret the primary, and then how long to wait before deleting the old secret. Such a tool could also be configured to create and wait for migration jobs, to allow data encrypted at REST to be decrypted and rencrypted using the new key.

## Pros and cons

### Pros

Many of the pros are self evident in the explanation above, but are a few more that I can think of:

#### Not Kubernetes specific

The way code consumes keys is not at all specific to Kubernetes. It can work on any platform that can pass keys using a filesystem. This includes development environments where perhaps static keys might be used.

#### Kubernetes native

In spite of not being specific to Kubernetes, this mechanism is native to the way Kubernetes works. It uses the in built secret mechanism, and it's workable without any third party tooling. It will work on any Kubernetes distribution, and if you already understand how Kubernetes secrets work, it's straight forward to understand how this works.

#### No vendor lock in

This also provides for a vendor neutral way to rotate and consume certificates. Today, if using HashiCorp Vault for example, you need to use the Vault client in your code to connect to the Vault server to get keys, which ties your code to Vault. This convention allows whatever is managing the keys to be decoupled from the consumers. This also can be advantagous in development and test environments, you might not be able to run your vendors secrets manager on your local machine for example for licensing or cost reasons, so you can substitute in a different one in those environments.

### Cons

The convention is not without its cons of course.

#### Reliance on the filesystem

Some people may object to using the filesystem for distributing secrets, preferring to only pass them through authenticated connections. Of course, at some point some secret needs to be passed to the code - if the code is going to authenticate with a third party secret manager to retrieve secrets, the secret for that authentication needs to be stored somewhere, such as the filesystem or an environment variable.

#### Reliance or Kubernetes secrets

Some people may be concerned with the way Kubernetes stores secrets itself. As I understand, I think this is either pluggable, or can be encrypted (I know GKE supports integration with Cloud KMS to encrypt the secrets stored by Kubernetes, for example). But in some circumstances this might not be good enough for people.

#### Changes to the way code consumes secrets

The requirement to read secrets from the filesystem may be disruptive for libraries that typically consume secrets from configuration files or environment variables.

## Conclusion

So, does this convention sound useful? Please comment if you have anything to add!

