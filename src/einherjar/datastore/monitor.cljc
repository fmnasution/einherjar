(ns einherjar.datastore.monitor
  (:require
   [mount.core :refer [defstate]]
   [datascript-schema.core :as datascript.schema]
   [taoensso.timbre :as timbre]
   [einherjar.async.event :as asnc.evt]
   [einherjar.datastore.connection :as dtst.conn]
   [einherjar.datastore.initial :as dtst.init]
   #?@(:clj  [[clojure.core.async :as async]
              [datomic.api :as datomic]]
       :cljs [[cljs.core.async :as async]])))

;; ---- datastore tx monitor ----

(defrecord DatastoreTxMonitor [tx-report-chan stopper])

#?(:clj
   (defn- monitor-datomic-tx!
     [conn tx-report-chan]
     (let [active?_        (atom true)
           tx-report-queue (datomic/tx-report-queue conn)]
       (future
         (while @active?_
           (let [tx-report (.take tx-report-queue)]
             (async/put! tx-report-chan tx-report))))
       #(reset! active?_ false))))

(defn- monitor-datascript-tx!
  [conn tx-report-chan]
  (let [handler #(async/put! tx-report-chan %)]
    (datascript.schema/listen-on-schema-change! conn handler)
    #(datascript.schema/unlisten-schema-change! conn)))

(defn- start-datastore-tx-monitor!
  ([datastore-connection datastore-bootstrapper tx-report-chan]
   (let [kind    (dtst.conn/kind datastore-connection)
         conn    (dtst.conn/internal datastore-connection)
         stopper (case kind
                   :datomic
                   #?(:clj  (monitor-datomic-tx! conn tx-report-chan)
                      :cljs nil)

                   :datascript
                   (monitor-datascript-tx! conn tx-report-chan))]
     (->DatastoreTxMonitor tx-report-chan stopper)))
  ([datastore-connection datastore-bootstrapper]
   (start-datastore-tx-monitor! datastore-connection
                                datastore-bootstrapper
                                (async/chan 100))))

(defn- stop-datastore-tx-monitor!
  [{:keys [tx-report-chan stopper] :as datastore-tx-monitor}]
  (async/close! tx-report-chan)
  (stopper))

(defstate datastore-tx-monitor
  :start (do (timbre/info "Monitoring tx on datastore connection...")
             (start-datastore-tx-monitor! @dtst.conn/datastore-connection
                                          @dtst.init/datastore-bootstrapper))
  :stop  (do (timbre/info "No longer monitors tx on datastore connection...")
             (stop-datastore-tx-monitor! @datastore-tx-monitor)))

;; ---- datastore tx pipeliner ----

(defn- process-tx-report
  [datastore-connection]
  (fn [tx-report]
    (dtst.conn/process-tx-report datastore-connection tx-report)))

(defn- tx-report->event
  [tx-report]
  [:datastore-connection/tx-report tx-report])

(defstate datastore-tx-pipeliner
  :start (do (timbre/info "Pipelining tx"
                          "from datastore connection"
                          "to event dispatcher...")
             (async/pipeline
              1
              (:event-chan @asnc.evt/event-dispatcher)
              (comp
               (map (process-tx-report @dtst.conn/datastore-connection))
               (map tx-report->event))
              (:tx-report-chan @datastore-tx-monitor)
              (fn [error]
                [:datastore-tx-pipeliner/error
                 {:error error}
                 {:error? true}]))))
