from tornado.gen import coroutine

from .handler_base import ShopHandler

class ProductHandler(ShopHandler):
    @coroutine
    def get(self, id):
        item = yield self._get_json('catalog/item/%s' % id)

        self.render(
            'product.html',
            item=item,
            ref=self.request.headers.get('Referer', "/"))
