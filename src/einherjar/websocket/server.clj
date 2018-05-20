(ns einherjar.websocket.server
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.core.async :as async]
   [mount.core :refer [defstate]]
   [taoensso.sente.packers.transit :refer [get-transit-packer]]
   [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
   [taoensso.sente :as sente]
   [taoensso.timbre :as timbre]
   [taoensso.encore :as encore]
   [einherjar.async.event :as asnc.evt]))

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
  :stop  (do (timbre/info "Stopping websocket server...")
             (async/close! (:recv-chan @websocket-server))))

(defn ring-resource
  [{:keys [ring-ajax-get ring-ajax-post] :as websocket-server} request-method]
  (case request-method
    :get  ring-ajax-get
    :post ring-ajax-post
    nil))

(defn wrap-websocket-server
  [handler websocket-server]
  (fn [request]
    (handler (assoc request ::websocket-server websocket-server))))

(defn publish!
  [{:keys [send! connected-uids] :as websocket-server}
   {:keys [websocket/?reply-fn websocket/?reply-data event peer-id]
    :as   option}]
  (spec/assert ::publish-option option)
  (encore/cond!
   (and (some? ?reply-fn) (some? ?reply-data))
   (?reply-fn ?reply-data)

   (and (some? event) (some? peer-id))
   (send! peer-id event)

   (some? event)
   (let [peer-ids (:any @connected-uids)]
     (run! #(send! % event) peer-ids))))

;; ---- websocket server pipeliner ----

(defn- remote-event->local-event
  [{:keys [id ?data ring-req uid client-id ?reply-fn]
    :as   remote-event}]
  [id {:websocket/?data        ?data
       :websocket/ring-request ring-req
       :websocket/peer-id      uid
       :websocket/device-id    client-id
       :websocket/?reply-fn    ?reply-fn}])

(defstate websocket-server-pipeliner
  :start (do (timbre/info "Pipelining remote event"
                          "from websocket server"
                          "to event dispatcher...")
             (async/pipeline
              1
              (:event-chan @asnc.evt/event-dispatcher)
              (map remote-event->local-event)
              (:recv-chan @websocket-server)
              (fn [error]
                [:websocket-server-pipeliner/error
                 {:error error}
                 {:error? true}]))))

;; ---- spec ----

(spec/def :websocket/?reply-fn
  fn?)

(spec/def :websocket/?reply-data
  (spec/and map? seq))

(spec/def ::event
  ::asnc.evt/command)

(spec/def ::peer-id
  encore/nblank-str?)

(spec/def ::reply-option
  (spec/keys :req [:websocket/?reply-fn :websocket/?data]))

(spec/def ::send-option
  (spec/keys :req-un [::event ::peer-id]))

(spec/def ::broadcast-option
  (spec/keys :req-un [::event]))

(spec/def ::publish-option
  (spec/or :reply     ::reply-option
           :send      ::send-option
           :broadcast ::broadcast-option))
