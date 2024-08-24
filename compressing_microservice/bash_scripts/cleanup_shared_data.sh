#!/bin/bash
SHARED_DATA_DIR="/shared_data/"
if [ -d "$SHARED_DATA_DIR" ]; then
  find "$SHARED_DATA_DIR" -type f -mmin +720 -exec rm -f {} \;
fi
