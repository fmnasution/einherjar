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
   [einherjar.async.event :as asnc.evt]))

;; ---- websocket client ----

(defrecord WebsocketClient [event-dispatcher chsk recv-chan send! state])

(defn- start-websocket-client!
  [config event-dispatcher]
  (let [{:keys [uri]} (spec/assert ::websocket-client-config config)

        {:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket!
         uri
         {:type           :auto
          :packer         (get-transit-packer)
          :wrap-recv-evs? false})]
    (->WebsocketClient event-dispatcher chsk ch-recv send-fn state)))

(defn- stop-websocket-client!
  [{:keys [recv-chan chsk] :as websocket-client}]
  (sente/chsk-disconnect! chsk)
  (async/close! recv-chan))

(defstate websocket-client
  :start (do (timbre/info "Starting websocket client...")
             (start-websocket-client!
              (:websocket-client @cfg.clt/config)
              @asnc.evt/event-dispatcher))
  :stop  (do (timbre/info "Stopping websocket client...")
             (stop-websocket-client! @websocket-client)))

(defn csrf-token
  [{:keys [state] :as websocket-client}]
  (:csrf-token @state))

(defn- reply-handler
  [event-dispatcher event error-event]
  (fn [reply-data]
    (let [[id data]      (if (sente/cb-success? reply-data)
                           event
                           error-event)
          selected-event [id (assoc data :websocket/?data reply-data)]]
      (asnc.evt/dispatch! event-dispatcher selected-event))))

(defn- process-more
  [event-dispatcher [timeout-ms event error-event :as more]]
  (if (and (some? timeout-ms) (some? event) (some? error-event))
    (let [reply-handler (reply-handler event-dispatcher event error-event)]
      [timeout-ms reply-handler])
    more))

(defn publish!
  [{:keys [send! event-dispatcher] :as websocket-client}
   {:keys [event more] :as option}]
  (spec/assert ::publish-option option)
  (apply send! event (process-more event-dispatcher more)))

;; ---- websocket client pipeliner ----

(defn- remote-event->local-event
  [{:keys [event] :as remote-event}]
  (let [[id data] event]
    [id {:websocket/?data data}]))

(defstate websocket-client-pipeliner
  :start (do (timbre/info "Pipelining remote event"
                          "from websocket client"
                          "to event dispatcher...")
             (async/pipeline
              1
              (:event-chan @asnc.evt/event-dispatcher)
              (map remote-event->local-event)
              (:recv-chan @websocket-client)
              (fn [error]
                [:websocket-client-pipeliner/error
                 {:error error}
                 {:error? true}]))))

;; ---- spec ----

(spec/def ::uri
  encore/nblank-str?)

(spec/def ::websocket-client-config
  (spec/keys :req-un [::uri]))

(spec/def ::event
  ::asnc.evt/command)

(spec/def ::send-option
  (spec/keys :req-un [::event]))

(spec/def ::more
  (spec/tuple encore/pos-int?
              ::asnc.evt/command
              ::asnc.evt/command))

(spec/def ::reply-option
  (spec/keys :req-un [::event ::more]))

(spec/def ::publish-option
  (spec/or :reply ::reply-option
           :send  ::send-option))
