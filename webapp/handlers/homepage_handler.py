from tornado.gen import coroutine

from .handler_base import ShopHandler

class HomepageHandler(ShopHandler):
    @coroutine
    def get(self):
        self.render('index.html')
