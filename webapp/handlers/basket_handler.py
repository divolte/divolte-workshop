from tornado.gen import coroutine

from .handler_base import ShopHandler

class BasketHandler(ShopHandler):
    @coroutine
    def post(self):
        if (self.get_body_argument('action') == 'add'):
            item_id = self.get_body_argument('item_id')
            yield self._uri_encoded_put_json('basket/%s' % self.shopper, item_id=item_id)
            self.write('OK')
        elif (self.get_body_argument('action') == 'delete'):
            item_id = self.get_body_argument('item_id')
            yield self._delete_json('basket/%s/%s' % (self.shopper, item_id))
            self.write('OK')
        else:
            raise ValueError('Unknown action!')

    @coroutine
    def get(self):
        basket = yield self._get_json('basket/%s' % self.shopper)
        self.render(
            'basket.html',
            basket=basket,
            ref=self.request.headers.get('Referer', "/"))
