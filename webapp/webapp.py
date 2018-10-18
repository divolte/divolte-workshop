from tornado import web,httpserver,ioloop
import argparse
import os

from handlers import *

import config

routes = [
    web.URLSpec(r'/', HomepageHandler, { 'config': config }),
    web.URLSpec(r'/category/([a-z]+)/(?:([0-9]{1,3})/)?', CategoryHandler, { 'config': config }),
    web.URLSpec(r'/product/([0-9]{6,15})/', ProductHandler, { 'config': config }),
    web.URLSpec(r'/basket/', BasketHandler, { 'config': config }),
    web.URLSpec(r'/checkout/', CheckoutHandler, { 'config': config }),
    web.URLSpec(r'/captcha/([0-9]{1,3})/', ImageHandler, { 'config': config }),
    web.URLSpec(r'/download/([a-f0-9]{32,60})/', DownloadHandler, { 'config': config }),
    web.URLSpec(r'/search', SearchHandler, { 'config': config })
]

def main(args):
    application = web.Application(
        routes,
        debug=args.debug,
        cookie_secret=args.secret,
        template_path='templates',
        static_path='static'
        )
    server = httpserver.HTTPServer(application)
    server.bind(args.port, address=args.address)
    server.start()
    ioloop.IOLoop.current().start()


def parse_args():
    parser = argparse.ArgumentParser(description='Runs the webapp.')
    parser.add_argument('--port', '-p', metavar='PORT', type=int, required=False, default=8989, help='The TCP port to listen on for HTTP requests.')
    parser.add_argument('--address', '-a', metavar='BIND_ADDRESS', type=str, required=False, default='127.0.0.1', help='The address to bind on.')
    parser.add_argument('--secret', '-s', metavar='COOKIE_SECRET', type=str, required=True, help='Random string to use for HMAC-ing cookies.')
    parser.add_argument('--debug', '-d', dest='debug', action='store_true', required=False, default=False, help='Run in debug mode.')
    return parser.parse_args()


if __name__ == '__main__':
    main(parse_args())
