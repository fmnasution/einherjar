(ns einherjar.async.effect
  (:require
   [mount.core :refer [defstate]]
   [taoensso.timbre :as timbre]
   [taoensso.encore :as encore]
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
              {}))
  :stop  (do (timbre/info "Stopping effect executor...")
             (stop-effect-executor! @effect-executor)))

(defn reg-effect
  [id handler]
  (defmethod -execute-effect! id
    [services effect]
    (handler services effect)))

;; ---- event dispatcher pipeliner ----

(defmulti -event->effects dispatch-by-id)

(defn event->effects
  [datastore-connection]
  (fn [event]
    (let [datastore-database (dtst.conn/db datastore-connection)]
      (not-empty (-event->effects datastore-database event)))))

(defstate event-dispatcher-pipeliner
  :start (do (timbre/info "Pipelining event"
                          "from event dispatcher"
                          "to effect executor...")
             (async/pipeline 1
                             (:effect-chan @effect-executor)
                             (mapcat
                              (event->effects @dtst.conn/datastore-connection))
                             (:event-chan @asnc.evt/event-dispatcher)
                             false
                             (fn [error]
                               [::error {:error error}]))))

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
