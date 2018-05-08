(ns einherjar.datastore.impl.datomic
  (:require
   [clojure.spec.alpha :as spec]
   [datomic.api :as datomic]
   [taoensso.encore :as encore]
   [einherjar.datastore.protocols :as dtst.prt]))

;; ---- datomic database ----

(defrecord DatomicDatabase [db]
  dtst.prt/IDatastore
  (kind [_] :datomic)
  (internal [this] (:db this))

  dtst.prt/IDatastoreDatabase
  (q [{:keys [db]} query args]
    (apply datomic/q query db args))
  (entity [{:keys [db]} eid]
    (datomic/entity db eid)))

;; ---- datomic connection ----

(defrecord DatomicConnection [conn]
  dtst.prt/IDatastore
  (kind [_] :datomic)
  (internal [this] (:conn this))

  dtst.prt/IDatastoreConnection
  (transact [{:keys [conn]} tx-data tx-meta]
    (datomic/transact conn tx-data))
  (transact [this tx-data]
    (dtst.prt/transact this tx-data {}))
  (db [{:keys [conn]}]
    (->DatomicDatabase (datomic/db conn))))

(defn start-datomic-connection!
  [config]
  (let [{:keys [uri]} (spec/assert ::datomic-connection-config config)
        created?      (datomic/create-database uri)
        conn          (datomic/connect uri)]
    (->DatomicConnection conn)))

(defn release-datomic-connection!
  [{:keys [conn] :as datomic-connection}]
  (datomic/release conn))

;; ---- spec ----

(spec/def ::uri
  encore/nblank-str?)

(spec/def ::datomic-connection-config
  (spec/keys :req-un [::uri]))
