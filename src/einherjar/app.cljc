(ns einherjar.app
  (:require
   [mount.core :as mount]
   [einherjar.async.effect]
   [einherjar.async.event]
   [einherjar.datastore.connection]
   [einherjar.datastore.monitor]
   [einherjar.async.handler]
   [einherjar.websocket.handler]
   [einherjar.datastore.handler]
   [einherjar.web.handler]
   [einherjar.router.handler]
   #?@(:clj  [[einherjar.config.server]
              [einherjar.web.server]
              [einherjar.router.server]
              [einherjar.websocket.server]]
       :cljs [[einherjar.config.client]
              [einherjar.element.react]
              [einherjar.router.client]
              [einherjar.websocket.client]
              [einherjar.web.ajax.client]
              [einherjar.element.handler]])))

(mount/in-cljc-mode)

#?(:cljs
   (enable-console-print!))

#?(:cljs
   (mount/start))
