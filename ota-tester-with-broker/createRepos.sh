#!/bin/sh
docker run --name mongodb -d -p 27017:27017 mongo
#sudo docker run --name minio -p 9000:9000 -d -p 9001:9001 -e "MINIO_ROOT_USER=minio99" -e "MINIO_ROOT_PASSWORD=minio123" quay.io/minio/minio server /data --console-address ":9001"
docker run --name minio -d --publish 9000:9000 --publish 9001:9001 --env MINIO_DEFAULT_BUCKETS='imagerepo:public' --env MINIO_ROOT_USER="minio99" --env MINIO_ROOT_PASSWORD="minio123"  bitnami/minio:latest
