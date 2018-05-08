(ns einherjar.websocket.server
  (:require
   [clojure.core.async :as async]
   [mount.core :refer [defstate]]
   [taoensso.sente.packers.transit :refer [get-transit-packer]]
   [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
   [taoensso.sente :as sente]
   [taoensso.timbre :as timbre]))

;; ---- websocket server ----

(defrecord WebsocketServer [ring-ajax-get
                            ring-ajax-post
                            recv-chan
                            send!
                            connected-uids])

(defn- start-websocket-server!
  []
  (let [{:keys [ch-recv
                send-fn
                connected-uids
                ajax-post-fn
                ajax-get-or-ws-handshake-fn]}

        (sente/make-channel-socket!
         (get-sch-adapter)
         {:packer (get-transit-packer)})]
    (->WebsocketServer ajax-get-or-ws-handshake-fn
                       ajax-post-fn
                       ch-recv
                       send-fn
                       connected-uids)))

(defstate websocket-server
  :start (do (timbre/info "Starting websocket server...")
             (start-websocket-server!))
  :stop (do (timbre/info "Stopping websocket server...")
            (async/close! (:recv-chan @websocket-server))))

(defn ring-resource
  [{:keys [ring-ajax-get ring-ajax-post] :as ws-server} request-method]
  (case request-method
    :get  ring-ajax-get
    :post ring-ajax-post
    nil))

(defn wrap-websocket-server
  [handler websocket-server]
  (fn [request]
    (handler (assoc request ::websocket-server websocket-server))))
