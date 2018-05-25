(ns einherjar.datastore.impl.datascript
  (:require
   [datascript.core :as datascript]
   [taoensso.encore :as encore]
   [einherjar.datastore.protocols :as dtst.prt]
   #?@(:clj [[clojure.spec.alpha :as spec]]
       :cljs [[cljs.spec.alpha :as spec]])))

;; ---- datascript database ----

(declare -process-tx-report)

(defrecord DatascriptDatabase [db]
  dtst.prt/IDatastore
  (kind [_] :datascript)
  (internal [this] (:db this))
  (process-tx-report [_ tx-report]
    (-process-tx-report tx-report))

  dtst.prt/IDatastoreDatabase
  (q [{:keys [db]} query args]
    (apply datascript/q query db args))
  (entity [{:keys [db]} eid]
    (datascript/entity db eid))
  (entid [{:keys [db]} ident]
    (datascript/entid db ident)))

(defn new-datascript-database
  [db]
  (->DatascriptDatabase db))

;; ---- datascript connection ----

(defn- -process-tx-report
  [tx-report]
  (-> tx-report
      (update :db-after new-datascript-database)
      (update :db-before new-datascript-database)))

(defrecord DatascriptConnection [conn]
  dtst.prt/IDatastore
  (kind [_] :datascript)
  (internal [this] (:conn this))
  (process-tx-report [_ tx-report]
    (-process-tx-report tx-report))

  dtst.prt/IDatastoreConnection
  (transact [{:keys [conn]} tx-data tx-meta]
    (-> (datascript/transact conn tx-data tx-meta)
        (deref)
        (-process-tx-report)))
  (transact [this tx-data]
    (dtst.prt/transact this tx-data {}))
  (db [{:keys [conn]}]
    (new-datascript-database (datascript/db conn))))

(defn start-datascript-connection!
  [config]
  (let [{:keys [schema] :or {schema {}}}
        (spec/assert ::datascript-connection-config config)]
    (->DatascriptConnection (datascript/create-conn schema))))

;; ---- spec ----

(spec/def ::inner-schema
  (spec/and (spec/map-of encore/qualified-keyword?
                         encore/qualified-keyword?)
            seq))

(spec/def ::schema
  (spec/map-of encore/qualified-keyword? ::inner-schema))

(spec/def ::datascript-connection-config
  (spec/keys :opt-un [::schema]))
