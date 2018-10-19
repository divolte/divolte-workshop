mapping {
    map firstInSession() onto 'firstInSession'
    map timestamp() onto 'timestamp'
    map clientTimestamp() onto 'clientTimestamp'
    map remoteHost() onto 'remoteHost'
    map referer() onto 'referer'
    map location() onto 'location'
    map partyId() onto 'partyId'
    map sessionId() onto 'sessionId'
    map pageViewId() onto 'pageViewId'
    map eventId() onto 'eventId'
    map eventType() onto 'eventType'

    // EXERCISE 2: INSERT SECTION HERE
    section {
        def locationUri = parse location() to uri
        def locationPath = locationUri.path()
        when locationPath.equalTo('/') apply {
            map 'home' onto 'pageType'
            // Skip rest of section.
            exit()
        }
        // Regular expression for extracting pieces out of the path.
        def pathMatcher = match '^/([a-z]+)/([^/]+)?[/]?([^/]+)?.*' against locationPath
        when pathMatcher.matches() apply {
            map pathMatcher.group(1) onto 'pageType'
            when pathMatcher.group(1).equalTo('category') apply {
                map pathMatcher.group(2) onto 'category'
                when pathMatcher.group(3).isAbsent() apply {
                    map 0 onto 'page'
                }
                map { parse pathMatcher.group(3) to int32 } onto 'page'
                exit()
            }
            when pathMatcher.group(1).equalTo('product') apply {
                map pathMatcher.group(2) onto 'productId'
                exit()
            }
            when pathMatcher.group(1).equalTo('download') apply {
                map pathMatcher.group(2) onto 'downloadId'
                exit()
            }
            exit()
        }
    }

    // EXERCISE 3: INSERT SECTION HERE
    section {
        when eventType().equalTo('removeFromBasket') apply {
            map eventParameters().value('item_id') onto 'productId'
            exit()
        }

        when eventType().equalTo('addToBasket') apply {
            map eventParameters().value('item_id') onto 'productId'
            exit()
        }

        when eventType().equalTo('preview') apply {
            map eventParameters().value('item_id') onto 'productId'
            exit()
        }
    }
}
