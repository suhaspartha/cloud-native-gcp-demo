#!/bin/bash
set -e
PROJECT_ID=$(gcloud config get-value project)
REGION=europe-west1

echo ">> Stopping Cloud SQL (if exists)..."
gcloud sql instances patch demo-db \
  --activation-policy=NEVER --quiet 2>/dev/null \
  && echo "   Cloud SQL stopped" \
  || echo "   Cloud SQL not found, skipping"

echo ">> Deleting GKE cluster (if exists)..."
gcloud container clusters delete demo-cluster \
  --region=$REGION --quiet 2>/dev/null \
  && echo "   GKE cluster deleted" \
  || echo "   GKE not found, skipping"

echo ""
echo ">> Active resources (free tier — safe to leave running):"
gcloud run services list --region=$REGION
