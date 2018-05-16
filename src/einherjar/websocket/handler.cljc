(ns einherjar.websocket.handler
  (:require
   [einherjar.async.effect :as asnc.efc]
   #?@(:clj  [[einherjar.websocket.server :as ws.srv]]
       :cljs [[einherjar.websocket.client :as ws.clt]])))

;; ---- event handler ----

#?(:clj
   (asnc.efc/reg-event
    :websocket-server-pipeliner/error
    (fn [_ data]
      [[:logger/error data]]))
   :cljs
   (asnc.efc/reg-event
    :websocket-client-pipeliner/error
    (fn [_ data]
      [[:logger/error data]])))

;; ---- effect handler ----

#?(:clj
   (asnc.efc/reg-effect
    :websocket-server/publish
    (fn [{:keys [websocket-server]} [_ option]]
      (ws.srv/publish! websocket-server option)))
   :cljs
   (asnc.efc/reg-effect
    :websocket-client/publish
    (fn [{:keys [websocket-client]} [_ option]]
      (ws.clt/publish! websocket-client option))))
