from tornado import web
from tornado.gen import coroutine
from tornado.httpclient import HTTPRequest,AsyncHTTPClient
from tornado.escape import json_decode

from itertools import zip_longest
from uuid import uuid4
from urllib.parse import urlencode

import config

class ShopHandler(web.RequestHandler):
    shopper = None
    config = None
    
    def prepare(self):
        self.shopper = self.get_cookie('shopper') or uuid4().hex
        self.set_cookie('shopper', self.shopper, expires_days=730, domain=self.config.COOKIE_DOMAIN)

    def initialize(self, config):
        self.config = config

    __initialize = initialize

    # Templating helpers
    def __grouper(self, iterable, n, fillvalue=None):
        args = [iter(iterable)] * n
        return zip_longest(*args, fillvalue=fillvalue)

    def get_template_namespace(self):
        return {
            'grouper': self.__grouper,
            'config': self.config,
            'shopper': self.shopper
        }

    # HTTP Helpers
    @coroutine
    def _get_json(self, path, **kwargs):
        http = AsyncHTTPClient()
        request = HTTPRequest(url='%s/%s?%s' % (self.config.API_URL, path, urlencode(kwargs, doseq=True)), method='GET')
        response = yield http.fetch(request)
        return json_decode(response.body)

    @coroutine
    def _get_binary(self, path, **kwargs):
        http = AsyncHTTPClient()
        request = HTTPRequest(url='%s/%s?%s' % (self.config.API_URL, path, urlencode(kwargs, doseq=True)), method='GET')
        response = yield http.fetch(request)
        return response.body

    @coroutine
    def _uri_encoded_put_json(self, path, **kwargs):
        return self._uri_encoded_body_json(path, 'PUT', **kwargs)

    @coroutine
    def _uri_encoded_post_json(self, path, **kwargs):
        return self._uri_encoded_body_json(path, 'POST', **kwargs)

    @coroutine
    def _uri_encoded_body_json(self, path, method, **kwargs):
        http = AsyncHTTPClient()
        request = HTTPRequest(
            url='%s/%s' % (self.config.API_URL, path),
            method=method,
            headers={ 'Content-Type':'application/x-www-form-urlencoded' },
            body=urlencode(kwargs, doseq=True)
            )
        response = yield http.fetch(request)
        return json_decode(response.body)

    @coroutine
    def _delete_json(self, path):
        http = AsyncHTTPClient()
        request = HTTPRequest(
            url='%s/%s' % (self.config.API_URL, path),
            method='DELETE')
        response = yield http.fetch(request)
        return json_decode(response.body)
