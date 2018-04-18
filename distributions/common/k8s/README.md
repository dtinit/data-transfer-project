# Kubernetes

This directory contains configuration for Data Transfer Project to run
with Kubernetes using the Docker container format.

## First-time kubectl setup 

Kubernetes' command line utility, kubectl, must be authenticated and configured to point to your cluster(s).

### Setup for an existing Azure project

To configure kubectl to connect to your Kubernetes cluster, run the `az acs kubernetes get-credentials` command. This downloads credentials and configures kubectl to use them.

Detailed instructions are available in Azure [docs](https://docs.microsoft.com/en-us/azure/container-service/kubernetes/container-service-kubernetes-walkthrough#connect-to-the-cluster).

### Setup for an existing GKE (Google Container Engine) project

To configure kubectl to connect to your Kubernetes cluster, run the `gcloud container clusters get-credentials` command. This downloads credentials and configures kubectl to use them.

```
> gcloud container clusters list 
NAME                          LOCATION       
portability-api-cluster       us-central1-a  
portability-transfer-cluster  us-central1-a  
> gcloud container clusters get-credentials portability-api-cluster --zone=us-central1-a
Fetching cluster endpoint and auth data.
kubeconfig entry generated for portability-api-cluster.
>gcloud container clusters get-credentials portability-transfer-cluster --zone=us-central1-a 
Fetching cluster endpoint and auth data.
kubeconfig entry generated for portability-transfer-cluster.
```
Your cluster will now be set to the most recent one you downloaded credentials for, i.e. portability-transfer-cluster

```
>kubectl config current-context
gke_world-takeout-qa_us-central1-a_portability-transfer-cluster
```

Detailed instructions are available in GCP [docs](https://cloud.google.com/sdk/gcloud/reference/container/clusters/get-credentials).

## Tailing the container logs

It is useful to view the logs of the API and/or transfer worker instances when debugging. Depending on the log interface your cloud platform provides, and the number of replicas you have, it may be easier to view logs directly from Kubernetes.

Below are instructions for tailing the **API** logs. In this example, we only have two replicas, so tailing the logs of just two containers is not unwieldy.

1. Make sure your kubectl context is set to the correct cluster
    * View the current context:
    ```
      > kubectl config current-context
      gke_acme-corp-qa_us-central1-a_portability-api-cluster
    ```
    * If this is not the API cluster for your project, find the context with:
    ```
      > kubectl config get-contexts
    ```
    * and set kubectl to use it with:
    ```
      > kubectl config use-context gke_acme-corp-qa_us-central1-a_portability-api-cluster
    ```
    For **transfer** logs, do the exact same thing but set your kubectl context to the transfer cluster rather than API.

1. Find the pods
    ```
    > kubectl get pods
    NAME                               READY     STATUS    RESTARTS   AGE
    portability-api-1708261597-2km2c   1/1       Running   0          1h
    portability-api-1708261597-hm2tr   1/1       Running   0          1h
    ```
1. Tail the logs of each pod (each in its own terminal)
    ```
    > kubectl logs portability-api-1708261597-2km2c -f
    ```
    The *-f* will stream the logs to your terminal, similar to tailing a log file.
