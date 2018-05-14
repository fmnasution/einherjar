(ns einherjar.websocket.client
  (:require
   [cljs.spec.alpha :as spec]
   [cljs.core.async :as async]
   [mount.core :refer [defstate]]
   [taoensso.sente.packers.transit :refer [get-transit-packer]]
   [taoensso.sente :as sente]
   [taoensso.timbre :as timbre]
   [taoensso.encore :as encore]
   [einherjar.config.client :as cfg.clt]
   [einherjar.async.protocols :as asnc.prt]
   [einherjar.async.pipeliner :as asnc.ppln]
   [einherjar.async.event :as asnc.evt]))

;; ---- websocket client ----

(defrecord WebsocketClient [chsk recv-chan send! state]
  asnc.prt/ISource
  (source-chan [websocket-client]
    (:recv-chan websocket-client)))

(defn- start-websocket-client!
  [config]
  (let [{:keys [uri]} (spec/assert ::websocket-client-config config)

        {:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket!
         uri
         {:type   :auto
          :packer (get-transit-packer)})]
    (->WebsocketClient chsk ch-recv send-fn state)))

(defn- stop-websocket-client!
  [{:keys [recv-chan chsk] :as websocket-client}]
  (sente/chsk-disconnect! chsk)
  (async/close! recv-chan))

(defstate websocket-client
  :start (do (timbre/info "Starting websocket client...")
             (start-websocket-client! (:websocket-client @cfg.clt/config)))
  :stop  (do (timbre/info "Stopping websocket client...")
             (stop-websocket-client! @websocket-client)))

;; ---- websocket client pipeliner ----

(defn- remote-event->local-event
  [{:keys [event] :as remote-event}]
  (let [[id data] event]
    [id {:ws/?data data}]))

(defstate websocket-client-pipeliner
  :start (do (timbre/info "Pipelining remote event from websocket client"
                          "to event dispatcher...")
             (asnc.ppln/create-pipeliner
              {:flow-to       @asnc.evt/event-dispatcher
               :xform-fn      #(map remote-event->local-event)
               :flow-from     @websocket-client
               :error-handler (fn [error]
                                [::error {:error error}])})))

;; ---- spec ----

(spec/def ::uri
  encore/nblank-str?)

(spec/def ::websocket-client-config
  (spec/keys :req-un [::uri]))
