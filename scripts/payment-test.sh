#!/usr/bin/env bash

set -u

BASE_URL="${BASE_URL:-http://localhost:8080}"
DISH_ID="${DISH_ID:-3}"
ORDERS="${ORDERS:-40}"

stock_of() {
  curl -s "$BASE_URL/dishes/$DISH_ID" | sed -n 's/.*"stock": *\([0-9]*\).*/\1/p'
}

before=$(stock_of)
echo "Stock before: $before"
codes=""
for i in $(seq 1 "$ORDERS"); do
  code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/orders" \
    -H 'Content-Type: application/json' -d "{\"dishId\":$DISH_ID,\"quantity\":1}")
  codes="$codes$code"$'\n'
  sleep 0.1
done

echo "Status codes (count, code):"
printf "%s" "$codes" | sort | uniq -c
confirmed=$(printf "%s" "$codes" | grep -c '^201$' || true)
after=$(stock_of)
echo "Confirmed: $confirmed | stock after: $after"

if [ $((before - after)) -eq "$confirmed" ]; then
  echo "RESULT: PASS - stock decreased exactly by the number of confirmed orders (failures kept the stock intact)"
else
  echo "RESULT: FAIL - stock changed by $((before - after)) but $confirmed orders were confirmed"
  exit 1
fi
