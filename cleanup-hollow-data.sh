#!/bin/bash

# Cleanup script for Hollow demo
# This script removes all Hollow data files to allow starting with a clean slate

echo "=================================="
echo "Hollow Data Cleanup Script"
echo "=================================="
echo ""

HOLLOW_DATA_DIR="./hollow-data"

if [ -d "$HOLLOW_DATA_DIR" ]; then
    echo "Found hollow-data directory at: $HOLLOW_DATA_DIR"

    # Count files before deletion
    FILE_COUNT=$(find "$HOLLOW_DATA_DIR" -type f | wc -l | tr -d ' ')
    DIR_SIZE=$(du -sh "$HOLLOW_DATA_DIR" 2>/dev/null | cut -f1)

    echo "Current size: $DIR_SIZE"
    echo "Files to be deleted: $FILE_COUNT"
    echo ""

    read -p "Are you sure you want to delete all Hollow data? (y/N) " -n 1 -r
    echo ""

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        rm -rf "$HOLLOW_DATA_DIR"
        echo "✅ Hollow data directory deleted successfully!"
        echo ""
        echo "You can now restart the producer to generate a fresh dataset."
        echo "Run: ./gradlew bootRun --args='--spring.profiles.active=producer'"
    else
        echo "❌ Cleanup cancelled."
    fi
else
    echo "⚠️  No hollow-data directory found. Nothing to clean up."
    echo ""
    echo "The directory will be created automatically when you start the producer."
fi

echo ""
echo "=================================="
