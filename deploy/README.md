# Deployment info

The cert-manager deployment manifest is vanilla - no changes.

The nginx-ingress deployment is the baremetal manifest taken from [here](https://kubernetes.github.io/ingress-nginx/deploy/#bare-metal), with the following changes:

* The `NodePort` service has been deleted.
* The `ConfigMap` has a small amount of custom config in it.
* The `Deployment` ports 80 and 443 have configured `hostPort`.

The reason for configuring `hostPort` and `hostIP` is that the only other alternative in LKE to bind to a publicly accessible port 80 and port 443 is to use a Linode NodeBalancer, which costs $10/month, doubling my hosting costs. Since I'm only running a single node k8s cluster, this doesn't make sense, I gain nothing from it but pay twice as much. So I bind to port 80 and 443 of that single node.

Upgrading k8s presents a challenge, since you have to "recycle" the nodes to do this, this means allocating a new IP address, which means I need to change DNS. In theory, it could work by doing the following:

* Upgrade control plane to latest version (note, LKE only upgrades one version at a time, so make sure when I do upgrade, I go through all upgrades until I'm at the latest).
* Scale the node pool up to 2.
* Wait until the second node has come online.
* Transfer the IP address of the old node to the new.
* Scale the node pool back down to 1 - this should hopefully move all the services onto the new node with the new k8s version, and delete the old node.

I tried it today and it didn't work, but I stuffed up half way through. I should try again next time. The result should be an outage of a minute or less.

If I ever scale up the cluster, I guess that might not work, not sure whether k8s will ensure the pod is deployed to the node with that IP address or not. And if I ever delete and create a new node, then I guess the IP address will change, and so I'll have to change the IP address configured there, and in DNS.
