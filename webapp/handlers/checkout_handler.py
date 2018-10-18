from tornado.gen import coroutine

from .handler_base import ShopHandler

class CheckoutHandler(ShopHandler):
    @coroutine
    def get(self):
        checkout = yield self._get_json('checkout/inflight/%s' % self.shopper)

        if checkout['step'] == 0:
            self.render(
                'checkout-1.html',
                checkout=checkout)
        elif checkout['step'] == 1:
            self.render(
                'checkout-2.html',
                checkout=checkout)
        elif checkout['step'] == 2:
            yield self._delete_json('checkout/inflight/%s' % self.shopper)
            self.redirect('/download/%s/' % checkout['id'])

    @coroutine
    def post(self):
        initial_checkout = yield self._get_json('checkout/inflight/%s' % self.shopper)

        step = int(self.get_body_argument('step'))

        if step == 1:
            email = self.get_body_argument('email')
            updated_checkout = yield self._uri_encoded_post_json('checkout/inflight/%s/1' % self.shopper, email=email)
        elif step == 2:
            responses = [self.get_body_argument('token%d' % i) for i in range(0, len(initial_checkout['tokens']))]
            updated_checkout = yield self._uri_encoded_post_json('checkout/inflight/%s/2' % self.shopper, responses=responses)

        self.redirect('/checkout/')
