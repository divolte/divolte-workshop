#!/usr/bin/env python3
import argparse
import requests
import json
import os

from functools import reduce
from collections import Counter


def make_item(json, filename):
    return {
        'id': json['id'],
        'title': json['title'],
        'description': json['info']['description'],
        'tags': json['info']['tags'],
        'favs': json['favs'],
        'owner': {
            'user_id': json['info']['owner']['id'],
            'user_name': json['info']['owner']['username'],
            'real_name': json['info']['owner']['realname']
        },
        'categories': [os.path.splitext(os.path.basename(filename))[0]],
        'variants': {
            name: {
                'width': size['width'],
                'height': size['height'],
                'flickr_url': size['url'],
                'img_source': size['source']
                }
            for name,size in json['sizes'].items()
        }
    }

def merge_items(*input):
    items = list(filter(lambda x: x, input))

    if len(items) == 0:
        return

    if len(set([item['id'] for item in items])) != 1:
        raise ValueError('Not merging items with different IDs')

    result = {}
    result.update(items[0])
    for item in items[1:]:
        result['categories'] = list(set(result['categories'] + item['categories']))

    return result

def with_price(item, min_favs, max_favs):
    r = (max_favs - min_favs + 1) / 2.0
    result = {
        'price': int((item['favs'] - min_favs) / r) + 1
    }
    result.update(item)

    return result

def main(args):
    print('Items from:\n\t%s' % '\n\t'.join(args.files))

    item_list = reduce(
        lambda x,y: x+y,
        [[
            make_item(json.loads(line), fn)
        for line in open(fn).readlines()]
        for fn in args.files])

    min_favs = min([item['favs'] for item in item_list])
    max_favs = max([item['favs'] for item in item_list])
    priced_item_list = [with_price(item, min_favs, max_favs) for item in item_list]

    items = {}
    for item in priced_item_list:
        items[item['id']] = merge_items(item, items.get(item['id']))

    responses = Counter()
    for item in items.values():
        r = requests.put(args.api_base_url + '/catalog/item', data=json.dumps(item), headers={'Content-Type': 'application/json'})
        print('PUT %s, %d' % (item['id'], r.status_code))
        responses.update([r.status_code])

    print('\nSummary:')
    for status, count in responses.items():
       print('\t%d times status code %d' % (count,status))

def parse_args():
    parser = argparse.ArgumentParser(description='Take downloaded category JSON files and push them into the Catalog API.')
    parser.add_argument('--api-base-url', '-u', metavar='API_BASE_URL', type=str, default='http://localhost:8080/api', help='The base URL of the Catalog API.')
    parser.add_argument('files', metavar='JSON_FILE', type=str, nargs='+')

    return parser.parse_args()

if __name__ == '__main__':
    args = parse_args()
    main(args)
