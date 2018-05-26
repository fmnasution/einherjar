(ns einherjar.datastore.connection
  (:require
   [mount.core :refer [defstate]]
   [datascript.core :as datascript]
   [taoensso.timbre :as timbre]
   [taoensso.encore :as encore]
   [einherjar.datastore.protocols :as dtst.prt]
   [einherjar.datastore.impl.datascript :as dtst.ipl.dts]
   #?@(:clj  [[clojure.spec.alpha :as spec]
              [clojure.core.async :as async]
              [datomic.api :as datomic]
              [einherjar.config.server :as cfg.srv]
              [einherjar.datastore.impl.datomic :as dtst.ipl.dtm]]
       :cljs [[cljs.spec.alpha :as spec]
              [cljs.core.async :as async]
              [einherjar.config.client :as cfg.clt]])))

;; ---- datastore database ----

(declare -process-tx-report)

(defrecord DatastoreDatabase [db]
  dtst.prt/IDatastore
  (kind [{:keys [db]}]
    (dtst.prt/kind db))
  (internal [{:keys [db]}]
    (dtst.prt/internal db))
  (process-tx-report [{:keys [db]} tx-report]
    (-process-tx-report (dtst.prt/process-tx-report db tx-report)))

  dtst.prt/IDatastoreDatabase
  (q [{:keys [db]} query args]
    (dtst.prt/q db query args))
  (entity [{:keys [db]} eid]
    (dtst.prt/entity db eid))
  (entid [{:keys [db]} ident]
    (dtst.prt/entid db ident)))

(defn- new-datastore-database
  [db]
  (->DatastoreDatabase db))

;; ---- datastore connection ----

(defn- -process-tx-report
  [tx-report]
  (-> tx-report
      (update :db-after new-datastore-database)
      (update :db-before new-datastore-database)))

(defrecord DatastoreConnection [conn]
  dtst.prt/IDatastore
  (kind [{:keys [conn]}]
    (dtst.prt/kind conn))
  (internal [{:keys [conn]}]
    (dtst.prt/internal conn))
  (process-tx-report [{:keys [conn]} tx-report]
    (-process-tx-report (dtst.prt/process-tx-report conn tx-report)))

  dtst.prt/IDatastoreConnection
  (transact [{:keys [conn]} tx-data tx-meta]
    (-process-tx-report (dtst.prt/transact conn tx-data tx-meta)))
  (transact [{:keys [conn]} tx-data]
    (dtst.prt/transact conn tx-data))
  (db [{:keys [conn]}]
    (new-datastore-database (dtst.prt/db conn))))

(defn- start-datastore-connection!
  [config]
  (let [{:keys [kind]} (spec/assert ::datastore-connection-config config)
        conn           (case kind
                         :datomic
                         #?(:clj  (dtst.ipl.dtm/start-datomic-connection! config)
                            :cljs nil)

                         :datascript
                         (dtst.ipl.dts/start-datascript-connection! config))]
    (->DatastoreConnection conn)))

(defn- stop-datastore-connection!
  [{:keys [conn] :as datastore-connection}]
  (when (= :datomic (dtst.prt/kind datastore-connection))
    #?(:clj  (dtst.ipl.dtm/release-datomic-connection! conn)
       :cljs nil)))

(defstate datastore-connection
  :start (do (timbre/info "Starting datastore connection...")
             (start-datastore-connection!
              (:datastore-connection
               #?(:clj  @cfg.srv/config
                  :cljs @cfg.clt/config))))
  :stop  (do (timbre/info "Stopping datastore connection...")
             (stop-datastore-connection! @datastore-connection)))

(defn kind
  [datastore]
  (dtst.prt/kind datastore))

(defn internal
  [datastore]
  (dtst.prt/internal datastore))

(defn process-tx-report
  [datastore tx-report]
  (dtst.prt/process-tx-report datastore tx-report))

(defn transact
  ([datastore-connection tx-data tx-meta]
   (dtst.prt/transact datastore-connection tx-data tx-meta))
  ([datastore-connection tx-data]
   (dtst.prt/transact datastore-connection tx-data)))

(defn db
  [datastore-connection]
  (dtst.prt/db datastore-connection))

(defn q
  [query datastore-database & args]
  (dtst.prt/q datastore-database query args))

(defn entity
  [datastore-database eid]
  (dtst.prt/entity datastore-database eid))

(defn entid
  [datastore-database ident]
  (dtst.prt/entid datastore-database ident))

(defn tempid
  ([kind prtn n]
   (case kind
     :datomic    #?(:clj  (if (integer? n)
                            (datomic/tempid prtn n)
                            (datomic/tempid prtn))
                    :cljs nil)
     :datascript (if (integer? n)
                   (datascript/tempid prtn n)
                   (datascript/tempid prtn))))
  ([kind prtn]
   (tempid kind prtn nil)))

;; ---- spec ----

(spec/def ::kind
  #{#?(:clj :datomic) :datascript})

(spec/def ::datastore-connection-config
  (spec/keys :req-un [::kind]))

(spec/def ::lookup-ref
  (spec/tuple encore/qualified-keyword? some?))

(spec/def ::real-eid
  encore/nneg-int?)

(spec/def ::datascript-temp-eid
  encore/neg-int?)

#?(:clj
   (spec/def ::datomic-temp-eid
     #(instance? datomic.db.DbId %)))

(spec/def ::temp-eid
  (spec/or #?@(:clj [:datomic ::datomic-temp-eid])
           :datascript ::datascript-temp-eid))
