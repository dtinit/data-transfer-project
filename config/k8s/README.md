# Kubernetes

This directory contains configuration for Data Portability to run
with Kubernetes using the Docker container format.

## Tailing the container logs

It is useful to view the logs of the API and/or worker instances when debugging. Depending on the log interface your cloud platform provides, and the number of replicas you have, it may be easier to view logs directly from Kubernetes.

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
    For **worker** logs, do the exact same thing but set your kubectl context to the worker cluster rather than API.

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
