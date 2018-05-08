(ns einherjar.datastore.monitor
  (:require
   [mount.core :refer [defstate]]
   [datascript.core :as datascript]
   [taoensso.timbre :as timbre]
   [taoensso.encore :as encore]
   [einherjar.async.event :as asnc.evt]
   [einherjar.datastore.protocols :as dtst.prt]
   [einherjar.datastore.connection :as dtst.conn]
   #?@(:clj [[clojure.core.async :as async]
             [datomic.api :as datomic]]
       :cljs [[cljs.core.async :as async]])))

;; ---- datastore tx monitor ----

(defrecord DatastoreTxMonitor [tx-report-chan stopper])

(encore/if-clj
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
  (datascript/listen! conn ::tx-report #(async/put! tx-report-chan %))
  #(datascript/unlisten! conn ::tx-report))

(defn- start-datastore-tx-monitor!
  ([{:keys [conn] :as datastore-connection} tx-report-chan]
   (let [kind    (dtst.prt/kind datastore-connection)
         stopper (case kind
                   :datomic
                   (encore/if-clj
                    (monitor-datomic-tx! conn tx-report-chan)
                    nil)

                   :datascript
                   (monitor-datascript-tx! conn tx-report-chan))]
     (->DatastoreTxMonitor tx-report-chan stopper)))
  ([datastore-connection]
   (start-datastore-tx-monitor! datastore-connection (async/chan 100))))

(defn- stop-datastore-tx-monitor!
  [{:keys [tx-report-chan stopper] :as datastore-tx-monitor}]
  (async/close! tx-report-chan)
  (stopper))

(defstate datastore-tx-monitor
  :start (do (timbre/info "Monitoring tx on datastore connection...")
             (start-datastore-tx-monitor! @dtst.conn/datastore-connection))
  :stop  (do (timbre/info "No longer monitors tx on datastore connection...")
             (stop-datastore-tx-monitor! @datastore-tx-monitor)))

;; ---- datastore tx pipeliner ----

(defstate datastore-tx-pipeliner
  :start (do (timbre/info "Pipelining tx from datastore connection"
                          "to event dispatcher...")
             (async/pipeline
              1
              (:event-chan @asnc.evt/event-dispatcher)
              (map (fn [tx-report]
                     [:datastore-connection/tx-report tx-report]))
              (:tx-report-chan @datastore-tx-monitor)
              false
              (fn [error]
                [::error {:error error}]))))
