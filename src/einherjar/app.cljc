(ns einherjar.app
  (:require
   [mount.core :as mount]
   [taoensso.encore :as encore]
   #?@(:clj [[einherjar.config.server]
             [einherjar.web.server]
             [einherjar.router.server]
             [einherjar.websocket.server]]
       :cljs [[einherjar.config.client]
              [einherjar.element.react]
              [einherjar.router.client]
              [einherjar.websocket.client]])
   [einherjar.async.effect]
   [einherjar.async.event]
   [einherjar.datastore.connection]
   [einherjar.datastore.monitor]))

(mount/in-cljc-mode)

(encore/if-cljs
 (enable-console-print!))

(encore/if-cljs
 (mount/start))
