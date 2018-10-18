import logging

from tornado.gen import coroutine
from tornado.httpclient import HTTPError

from .handler_base import ShopHandler


class SearchHandler(ShopHandler):

    @coroutine
    def get(self):
        log = logging.getLogger('SearchHandler')
        try:
            q = self.get_argument('q')
            page = self.get_argument('page', None)
            log.debug(f'Search, q={q}; page={page}')
            categories = yield self._get_json(
                'catalog/search',
                query=q,
                page=int(page) if page else 0,
                size=self.config.ITEMS_PER_PAGE)
            page = categories['page']
            items_per_page = self.config.ITEMS_PER_PAGE
            total = categories['total']
            query = q
            self.render(
                'category.html',
                items=categories['items'],
                page=page,
                items_per_page=self.config.ITEMS_PER_PAGE,
                total=total,
                query=query,
                prev_enabled=(page > 0),
                prev_url=f'/search?q={ query }&page={ page - 1 if page > 1 else 0}',
                next_enabled=(page < int(total / items_per_page)),
                next_url=f'/search?q={ query }&page={ page + 1 }',
                page_url=f'/search?q={ query }&page=%p')
        except HTTPError as e:
            log.error("Catalogue not found")
            if e.code == 404:
                self.render('catalog-not-initialised.html')
            else:
                raise e
