# Deployment info

The cert-manager deployment manifest is vanilla - no changes.

The nginx-ingress deployment is the baremetal manifest taken from [here](https://kubernetes.github.io/ingress-nginx/deploy/#bare-metal), with the following changes:

* The `NodePort` service has been deleted.
* The `ConfigMap` has a small amount of custom config in it.
* The `Deployment` ports 80 and 443 have configured `hostPort` and `hostIP`.

The reason for configuring `hostPort` and `hostIP` is that the only other alternative in LKE to bind to a publicly accessible port 80 and port 443 is to use a Linode NodeBalancer, which costs $10/month, doubling my hosting costs. Since I'm only running a single node k8s cluster, this doesn't make sense, I gain nothing from it but pay twice as much. So I bind to port 80 and 443 of the IP address of that single node.

If I ever scale up the cluster, I guess that might not work, not sure whether k8s will ensure the pod is deployed to the node with that IP address or not. And if I ever delete and create a new node, then I guess the IP address will change, and so I'll have to change the IP address configured there, and in DNS.

