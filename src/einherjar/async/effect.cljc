(ns einherjar.async.effect
  (:require
   [mount.core :refer [defstate]]
   [taoensso.timbre :as timbre]
   [taoensso.encore :as encore]
   [einherjar.async.event :as asnc.evt]
   [einherjar.datastore.connection :as dtst.conn]
   #?@(:clj [[clojure.core.async :as async :refer [go-loop]]
             [einherjar.config.server :as cfg.srv]
             [einherjar.websocket.server :as ws.srv]]
       :cljs [[cljs.core.async :as async]
              [einherjar.config.client :as cfg.clt]
              [einherjar.web.ajax.client :as wb.jx.clt]
              [einherjar.element.react :as el.rct]
              [einherjar.websocket.client :as ws.clt]]))
  #?(:cljs
     (:require-macros
      [cljs.core.async.macros :refer [go-loop]])))

(defn- dispatch-by-id
  [_ [id]]
  id)

(defn- error?
  [v]
  (boolean (when (vector? v)
             (let [[_ _ metadata] v]
               (:error? metadata false)))))

;; ---- effect executor ----

(defmulti -execute-effect! dispatch-by-id)

(defn- execute-effect!
  [services effect]
  (-execute-effect! services effect))

(defn- realizing-effect!
  [{:keys [event-chan] :as event-dispatcher} services effect-chan]
  (let [stop-chan (async/chan)]
    (go-loop []
      (encore/when-let [[effect chan]
                        (async/alts! [effect-chan stop-chan] :priority true)

                        continue?
                        (and (not= stop-chan chan) (some? effect))]
        (encore/when-let [services
                          (encore/map-vals deref services)

                          result
                          (encore/catching
                           (execute-effect! services effect)
                           error
                           [:effect-executor/error
                            {:error error}
                            {:error? true}])

                          error-happened?
                          (error? result)]
          (async/>! event-chan result))
        (recur)))
    stop-chan))

(defrecord EffectExecutor [effect-chan stop-chan])

(defn- start-effect-executor!
  ([event-dispatcher services effect-chan]
   (let [stop-chan (realizing-effect! event-dispatcher services effect-chan)]
     (->EffectExecutor effect-chan stop-chan)))
  ([event-dispatcher services]
   (start-effect-executor! event-dispatcher services (async/chan 100))))

(defn- stop-effect-executor!
  [{:keys [effect-chan stop-chan] :as effect-executor}]
  (async/close! stop-chan)
  (async/close! effect-chan))

(defstate effect-executor
  :start (do (timbre/info "Starting effect executor...")
             (start-effect-executor!
              @asnc.evt/event-dispatcher
              {:event-dispatcher     asnc.evt/event-dispatcher
               :datastore-connection dtst.conn/datastore-connection
               #?@(:clj [:websocket-server ws.srv/websocket-server
                         :config           cfg.srv/config]
                   :cljs [:websocket-client   ws.clt/websocket-client
                          :server-ajax-caller wb.jx.clt/server-ajax-caller
                          :rum-element        el.rct/rum-element
                          :config             cfg.clt/config])}))
  :stop  (do (timbre/info "Stopping effect executor...")
             (stop-effect-executor! @effect-executor)))

(defn reg-effect
  [id handler]
  (defmethod -execute-effect! id
    [services effect]
    (handler services effect)))

;; ---- event consumer ----

(defmulti -event->effects dispatch-by-id)

(defn- event->effects
  [datastore-connection]
  (fn [event]
    (let [datastore-database (dtst.conn/db datastore-connection)]
      (not-empty (-event->effects datastore-database event)))))

(defrecord EventConsumer [stop-chan])

(defn- start-event-consumer!
  [{:keys [event-chan] :as event-dispatcher}
   {:keys [effect-chan] :as effect-executor}
   datastore-connection]
  (let [stop-chan        (async/chan)
        event-to-effects (event->effects datastore-connection)]
    (go-loop []
      (encore/when-let [[event chan]
                        (async/alts! [event-chan stop-chan] :priority true)

                        continue?
                        (and (not= stop-chan chan) (some? event))]
        (encore/when-let [effects
                          (encore/catching
                           (event-to-effects event)
                           error
                           [[:event-consumer/error
                             {:error error}
                             {:error? true}]])

                          selected-chan
                          (encore/cond!
                           (every? error? effects)
                           event-chan

                           :let [not-error? (complement error?)]

                           (every? not-error? effects)
                           effect-chan)]
          (doseq [effect effects]
            (async/>! selected-chan effect))
          #_(run! #(async/>! selected-chan %) effects))
        (recur)))
    (->EventConsumer stop-chan)))

(defn- stop-event-consumer!
  [{:keys [stop-chan] :as event-consumer}]
  (async/close! stop-chan))

(defstate event-consumer
  :start (do (timbre/info "Starting event consumer...")
             (start-event-consumer! @asnc.evt/event-dispatcher
                                    @effect-executor
                                    @dtst.conn/datastore-connection))
  :stop  (do (timbre/info "Stopping event consumer...")
             (stop-event-consumer! @event-consumer)))

(defn reg-event
  [id handler]
  (defmethod -event->effects id
    [datastore-database event]
    (handler datastore-database event)))
