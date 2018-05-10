(ns einherjar.datastore.connection
  (:require
   [mount.core :refer [defstate]]
   [datascript.core :as datascript]
   [taoensso.timbre :as timbre]
   [taoensso.encore :as encore]
   [einherjar.datastore.protocols :as dtst.prt]
   [einherjar.datastore.impl.datascript :as dtst.ipl.dts]
   #?@(:clj [[clojure.spec.alpha :as spec]
             [clojure.core.async :as async]
             [datomic.api :as datomic]
             [einherjar.config.server :as cfg.srv]
             [einherjar.datastore.impl.datomic :as dtst.ipl.dtm]]
       :cljs [[cljs.spec.alpha :as spec]
              [cljs.core.async :as async]
              [einherjar.config.client :as cfg.clt]])))

;; ---- datastore database ----

(defrecord DatastoreDatabase [db]
  dtst.prt/IDatastore
  (kind [this]
    (dtst.prt/kind (:db this)))
  (internal [this]
    (dtst.prt/internal (:db this)))

  dtst.prt/IDatastoreDatabase
  (q [this query args]
    (dtst.prt/q (:db this) query args))
  (entity [this eid]
    (dtst.prt/entity (:db this) eid)))

;; ---- datastore connection ----

(defrecord DatastoreConnection [conn]
  dtst.prt/IDatastore
  (kind [this]
    (dtst.prt/kind (:conn this)))
  (internal [this]
    (dtst.prt/internal (:conn this)))

  dtst.prt/IDatastoreConnection
  (transact [this tx-data tx-meta]
    (dtst.prt/transact (:conn this) tx-data tx-meta))
  (transact [this tx-data]
    (dtst.prt/transact (:conn this) tx-data))
  (db [this]
    (->DatastoreDatabase (dtst.prt/db (:conn this)))))

(defn- start-datastore-connection!
  [config]
  (let [{:keys [kind]} (spec/assert ::datastore-connection-config config)
        conn           (case kind
                         :datomic
                         (encore/if-clj
                          (dtst.ipl.dtm/start-datomic-connection! config)
                          nil)

                         :datascript
                         (dtst.ipl.dts/start-datascript-connection! config))]
    (->DatastoreConnection conn)))

(defn- stop-datastore-connection!
  [{:keys [conn] :as datastore-conn}]
  (when (= :datomic (dtst.prt/kind datastore-conn))
    (encore/if-clj
     (dtst.ipl.dtm/release-datomic-connection! conn)
     nil)))

(defstate datastore-connection
  :start (do (timbre/info "Starting datastore connection...")
             (start-datastore-connection!
              (:datastore-connection
               (encore/if-clj @cfg.srv/config @cfg.clt/config))))
  :stop  (do (timbre/info "Stopping datastore connection...")
             (stop-datastore-connection! @datastore-connection)))

(defn db
  [datastore-connection]
  (dtst.prt/db datastore-connection))

;; ---- spec ----

(spec/def ::kind
  #{(encore/if-clj :datomic) :datascript})

(spec/def ::datastore-connection-config
  (spec/keys :req-un [::kind]))
