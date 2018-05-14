(ns einherjar.async.effect
  (:require
   [mount.core :refer [defstate]]
   [taoensso.timbre :as timbre]
   [taoensso.encore :as encore]
   [einherjar.async.protocols :as asnc.prt]
   [einherjar.async.pipeliner :as asnc.ppln]
   [einherjar.async.event :as asnc.evt]
   [einherjar.datastore.connection :as dtst.conn]
   #?@(:clj [[clojure.core.async :as async :refer [go-loop]]]
       :cljs [[cljs.core.async :as async]]))
  #?(:cljs
     (:require-macros
      [cljs.core.async.macros :refer [go-loop]])))

(defn- dispatch-by-id
  [_ [id]]
  id)

;; ---- effect executor ----

(defmulti -execute-effect! dispatch-by-id)

(defn- execute-effect!
  [services effect]
  (-execute-effect! services effect))

(defn- realizing-effect!
  [{:keys [event-chan] :as event-dispatcher} services effect-chan]
  (let [stop-chan (async/chan)]
    (go-loop []
      (let [[effect chan] (async/alts! [effect-chan stop-chan] :priority true)
            stop?         (or (= stop-chan chan) (nil? effect))]
        (when-not stop?
          (encore/catching
           (execute-effect! services effect)
           error
           (async/>! event-chan [::error {:error error}]))
          (recur))))
    stop-chan))

(defrecord EffectExecutor [effect-chan stop-chan]
  asnc.prt/ISink
  (sink-chan [effect-executor]
    (:effect-chan effect-executor)))

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
              {}))
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
  (let [stop-chan      (async/chan)
        event-consumer (event->effects datastore-connection)]
    (go-loop []
      (let [[event chan] (async/alts! [event-chan stop-chan] :priority true)
            stop?        (or (= stop-chan chan) (nil? event))]
        (when-not stop?
          (encore/catching
           (when-let [effects (event-consumer event)]
             (run! #(async/>! effect-chan %) effects))
           error
           (async/>! event-chan [::error {:error error}])))
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

;; ---- event handler ----

(reg-event
 :default
 (fn [_ [id]]
   (timbre/warn "Unknown event:" id)))

;; ---- effect handler ----

(reg-effect
 :default
 (fn [_ [id]]
   (timbre/warn "Unknown effect:" id)))

