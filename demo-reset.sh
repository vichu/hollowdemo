#!/usr/bin/env bash
set -euo pipefail

TERRAFORM_DIR="/Users/vranganathan/projects/hollow-infra-adapters/terraform/aws"

usage() {
  echo "Usage: $0 [--clean-only] [--teardown] [docker compose up args...]"
  echo ""
  echo "  (no flags)     Stop containers, clear S3/DynamoDB data, start fresh"
  echo "  --clean-only   Stop containers, clear S3/DynamoDB data, don't restart"
  echo "  --teardown     Stop containers, clear S3/DynamoDB data, destroy Terraform infra"
  exit 1
}

CLEAN_ONLY=false
TEARDOWN=false

for arg in "$@"; do
  case "$arg" in
    --clean-only) CLEAN_ONLY=true; shift ;;
    --teardown)   TEARDOWN=true; CLEAN_ONLY=true; shift ;;
    --help|-h)    usage ;;
  esac
done

# Load .env if present
if [ -f .env ]; then
  set -a && source .env && set +a
fi

: "${HOLLOW_AWS_REGION:?HOLLOW_AWS_REGION is not set}"
: "${HOLLOW_AWS_BUCKET:?HOLLOW_AWS_BUCKET is not set}"
: "${HOLLOW_AWS_DYNAMODB_TABLE:?HOLLOW_AWS_DYNAMODB_TABLE is not set}"
: "${HOLLOW_AWS_DATASET_ID:=movies}"

AWS_ARGS=(--region "$HOLLOW_AWS_REGION")

echo "==> Stopping running containers..."
docker compose down --remove-orphans 2>/dev/null || true

if [ "$TEARDOWN" = true ]; then
  echo "==> Purging all S3 object versions and delete markers in s3://${HOLLOW_AWS_BUCKET}/"
  VERSIONS=$(aws "${AWS_ARGS[@]}" s3api list-object-versions --bucket "$HOLLOW_AWS_BUCKET" \
    --query '{Objects: Versions[].{Key:Key,VersionId:VersionId}}' --output json 2>/dev/null)
  if [ "$(echo "$VERSIONS" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(len(d.get("Objects") or []))')" -gt 0 ]; then
    aws "${AWS_ARGS[@]}" s3api delete-objects --bucket "$HOLLOW_AWS_BUCKET" --delete "$VERSIONS" > /dev/null
  fi
  MARKERS=$(aws "${AWS_ARGS[@]}" s3api list-object-versions --bucket "$HOLLOW_AWS_BUCKET" \
    --query '{Objects: DeleteMarkers[].{Key:Key,VersionId:VersionId}}' --output json 2>/dev/null)
  if [ "$(echo "$MARKERS" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(len(d.get("Objects") or []))')" -gt 0 ]; then
    aws "${AWS_ARGS[@]}" s3api delete-objects --bucket "$HOLLOW_AWS_BUCKET" --delete "$MARKERS" > /dev/null
  fi
  echo "==> Bucket emptied (all versions removed). Destroying Terraform infrastructure..."
  cd "$TERRAFORM_DIR"
  terraform destroy -auto-approve
  echo "==> All AWS resources destroyed."
  exit 0
fi

echo "==> Clearing S3 blobs under s3://${HOLLOW_AWS_BUCKET}/${HOLLOW_AWS_DATASET_ID}/"
aws "${AWS_ARGS[@]}" s3 rm "s3://${HOLLOW_AWS_BUCKET}/${HOLLOW_AWS_DATASET_ID}/" --recursive || true

echo "==> Deleting DynamoDB announcement for dataset '${HOLLOW_AWS_DATASET_ID}'..."
aws "${AWS_ARGS[@]}" dynamodb delete-item \
  --table-name "$HOLLOW_AWS_DYNAMODB_TABLE" \
  --key "{\"dataset_id\": {\"S\": \"${HOLLOW_AWS_DATASET_ID}\"}}" || true

echo "==> Demo data cleared. AWS infrastructure (S3 bucket, DynamoDB table) is intact."

if [ "$CLEAN_ONLY" = true ]; then
  echo "==> Done. Run './demo-reset.sh' when ready to start the next demo."
  exit 0
fi

echo "==> Starting fresh demo..."
docker compose up --build "$@"
