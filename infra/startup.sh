#!/bin/bash
REGION=europe-west1

echo ">> Starting Cloud SQL (takes ~60s to be ready)..."
gcloud sql instances patch demo-db \
  --activation-policy=ALWAYS --quiet 2>/dev/null \
  && echo "   Cloud SQL starting..." \
  || echo "   Cloud SQL not found — Day 2 not set up yet"

echo ""
echo ">> Current Cloud Run services:"
gcloud run services list --region=$REGION
