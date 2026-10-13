#!/usr/bin/env bash

set -u

ORDER_URL="${ORDER_URL:-http://localhost:8080}"
REVIEW_URL="${REVIEW_URL:-http://localhost:8083}"
DISH_ID="${DISH_ID:-1}"
REVIEWS="${REVIEWS:-100}"

echo "Ranking before:"; curl -s "$REVIEW_URL/reviews/ranking"; echo
echo "Sending $REVIEWS reviews (ratings cycle 3,4,5)..."
codes=$(seq 1 "$REVIEWS" | xargs -P 20 -I{} sh -c \
  'curl -s -o /dev/null -w "%{http_code}\n" -X POST "$0/reviews" -H "Content-Type: application/json" -d "{\"dishId\":$1,\"rating\":$((($2 % 3) + 3)),\"comment\":\"load test\"}"' \
  "$ORDER_URL" "$DISH_ID" {})
echo "Status codes (count, code):"; echo "$codes" | sort | uniq -c

echo "Ranking right away (buffer not flushed yet, may be unchanged):"; curl -s "$REVIEW_URL/reviews/ranking"; echo
echo "Waiting 6 seconds for the scheduled flush..."; sleep 6
echo "Ranking after the flush:"; curl -s "$REVIEW_URL/reviews/ranking"; echo
