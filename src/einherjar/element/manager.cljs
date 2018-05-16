(ns einherjar.element.manager
  (:require
   [einherjar.datastore.protocols :as dtst.prt]
   [einherjar.datastore.connection :as dtst.conn]
   [einherjar.async.event :as asnc.evt]))

;; ---- manager ----

(defrecord Manager [datastore-database event-dispatcher])

(defn new-manager
  [datastore event-dispatcher]
  (let [datastore-database (if (satisfies? dtst.prt/IDatastoreDatabase
                                           datastore)
                             datastore
                             (dtst.conn/db datastore))]
    (->Manager datastore-database event-dispatcher)))

(defn subscribe
  [{:keys [datastore-database] :as manager} id & args]
  ;; TODO: implement this
  )

(defn dispatch!
  [{:keys [event-dispatcher] :as manager} event]
  (asnc.evt/dispatch! event-dispatcher event))
