#!/usr/bin/env bash
# Race-condition test: fires N simultaneous POST /orders for the promotion dish (id 1, stock 10).
# Expected on a freshly started order-service: exactly 10 x 201, no more, and stock 0 at the end.
# (409 = sold out. 429 can also appear if the bonus rate limit is enabled. 502 = payment failed 4 times.)
#
# Usage: ./scripts/race-test.sh            (defaults: 50 requests, dish 1, http://localhost:8080)
#        REQUESTS=80 DISH_ID=1 BASE_URL=http://192.168.0.10:8080 ./scripts/race-test.sh
set -u

BASE_URL="${BASE_URL:-http://localhost:8080}"
DISH_ID="${DISH_ID:-1}"
REQUESTS="${REQUESTS:-50}"

stock_of() {
  curl -s "$BASE_URL/dishes/$DISH_ID" | sed -n 's/.*"stock": *\([0-9]*\).*/\1/p'
}

echo "Stock before: $(stock_of)"
echo "Firing $REQUESTS simultaneous orders (dishId=$DISH_ID, quantity=1)..."

codes=$(seq 1 "$REQUESTS" | xargs -P "$REQUESTS" -I{} \
  curl -s -o /dev/null -w "%{http_code}\n" -X POST "$BASE_URL/orders" \
  -H 'Content-Type: application/json' -d "{\"dishId\":$DISH_ID,\"quantity\":1}")

echo
echo "Status codes (count, code):"
echo "$codes" | sort | uniq -c
confirmed=$(echo "$codes" | grep -c '^201$' || true)
stock_after=$(stock_of)

echo
echo "Confirmed orders (201): $confirmed"
echo "Stock after: $stock_after"

if [ "$confirmed" -eq 10 ] && [ "$stock_after" = "0" ]; then
  echo "RESULT: PASS - exactly 10 orders confirmed and stock ended at 0"
else
  echo "RESULT: FAIL - expected 10 confirmed orders and stock 0 (restart order-service before re-running)"
  exit 1
fi
