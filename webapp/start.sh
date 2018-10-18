#!/bin/bash

echo -n "CATEGORIES = [
  'animals',
  'cars',
  'flowers',
  'architecture',
  'landscape',
  'cities',
  'nautical'
]

API_URL = '${SHOP_API_URL}'

COOKIE_DOMAIN = ''

ITEMS_PER_PAGE = 30

DIVOLTE_URL='${SHOP_DIVOLTE_URL}'
" > /opt/shop/config.py

python /opt/shop/webapp.py --port ${SHOP_WEBAPP_PORT} --address '*' --secret ${SHOP_WEBAPP_SECRET}
