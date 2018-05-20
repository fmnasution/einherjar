(ns einherjar.websocket.handler
  (:require
   [taoensso.encore :as encore]
   [einherjar.async.effect :as asnc.efc]
   #?@(:clj  [[einherjar.websocket.server :as ws.srv]]
       :cljs [[einherjar.websocket.client :as ws.clt]])))

(defn- just-opened?
  [old-state new-state]
  (and (not (:open? old-state))
       (:open? new-state)))

(defn- first-time-opened?
  [old-state new-state]
  (and (just-opened? old-state new-state)
       (:first-open? new-state)))

(defn- real-first-time-opened?
  [old-state new-state]
  (and (not (:ever-opened? old-state))
       (first-time-opened? old-state new-state)))

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

#?(:cljs
   (asnc.efc/reg-event
    :chsk/state
    (fn [_ [_ {[old-state new-state] :websocket/?data}]]
      (encore/conj-when
       []
       (when (real-first-time-opened? old-state new-state)
         [:event-dispatcher/dispatch
          {:event [:datastore-connection/request-schema]}])
       (when (first-time-opened? old-state new-state)
         [:event-dispatcher/dispatch
          {:event [:websocket-client/request-myself]}])))))

#?(:cljs
   (asnc.efc/reg-event
    :websocket-client/error
    (fn [_ [_ data]]
      (println data))))

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
