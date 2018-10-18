from tornado.gen import coroutine

from .handler_base import ShopHandler

class ImageHandler(ShopHandler):
    @coroutine
    def get(self, token):
        checkout = yield self._get_json('checkout/inflight/%s' % self.shopper)
        image = yield self._get_binary('checkout/image', t=checkout['tokens'][int(token)])
        self.set_header('Content-Type', 'image/jpeg')
        self.write(image)
