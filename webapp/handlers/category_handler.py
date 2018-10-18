import logging

from tornado.gen import coroutine
from tornado.httpclient import HTTPError

from .handler_base import ShopHandler


class CategoryHandler(ShopHandler):
    @coroutine
    def get(self, name, page):
        try:
            categories = yield self._get_json(
                'catalog/category/%s' % name,
                page=int(page) if page else 0,
                size=self.config.ITEMS_PER_PAGE)

            name = categories['name']
            page = categories['page']
            items_per_page = self.config.ITEMS_PER_PAGE
            total = categories['total']
            self.render(
                'category.html',
                items=categories['items'],
                page=page,
                items_per_page=items_per_page,
                total=total,
                prev_enabled=(page > 0),
                prev_url=f'/category/{ name }/{ page - 1 if page > 1 else 0}/',
                next_enabled=(page < int(total / items_per_page)),
                next_url=f'/category/{ name }/{ page + 1 }/',
                page_url=f'/category/{ name }/%p/')
        except HTTPError as e:
            logging.error("Catalogue not found")
            if e.code == 404:
                self.render('catalog-not-initialised.html')
            else:
                raise e
