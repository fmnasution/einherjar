(ns einherjar.datastore.protocols)

(defprotocol IDatastore
  (kind [this])
  (internal [this]))

(defprotocol IDatastoreDatabase
  (q [this query args])
  (entity [this eid])
  (entid [this ident]))

(defprotocol IDatastoreConnection
  (transact
    [this tx-data tx-meta]
    [this tx-data])
  (db [this]))
