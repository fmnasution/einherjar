(ns einherjar.websocket.handler
  (:require
   [einherjar.async.effect :as asnc.efc]
   #?@(:clj [[einherjar.websocket.server :as ws.srv]]
       :cljs [[einherjar.websocket.client :as ws.clt]])))

;; ---- effect handler ----

(asnc.efc/reg-effect
 :websocket-remote/publish
 #?(:clj (fn [{:keys [websocket-server]} [_ option]]
           (ws.srv/publish! websocket-server option))
    :cljs (fn [{:keys [websocket-client]} [_ option]]
            (ws.clt/publish! websocket-client option))))
