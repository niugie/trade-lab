#!/usr/bin/env bash
# Demo: create order -> pay -> check inventory
BASE=http://localhost:8080

echo "== Create order =="
CREATE=$(curl -s -X POST "$BASE/api/v1/orders" \
  -H "Content-Type: application/json" \
  -d '{"userId":10001,"skuId":1,"quantity":1,"userCouponId":1,"idempotencyKey":"demo-'$(date +%s)'"}')
echo "$CREATE"
ORDER_ID=$(echo "$CREATE" | grep -o '"orderId":[0-9]*' | head -1 | cut -d: -f2)

echo "== Pay order $ORDER_ID =="
curl -s -X POST "$BASE/api/v1/orders/$ORDER_ID/pay" \
  -H "Content-Type: application/json" \
  -d '{"payNo":"PAY-DEMO-001"}' | jq .

echo "== Inventory =="
curl -s "$BASE/api/v1/inventory/1" | jq .
