(ns einherjar.datastore.util)

;; ---- tx meta ----

(defn should-sync?
  [{:keys [tx-meta] :as tx-report}]
  (:datastore-connection/sync? (:tx-meta tx-report) true))

(defn should-sync
  [tx-meta sync?]
  (assoc tx-meta :datastore-connection/sync? sync?))

(defn bootstrap-schema?
  [{:keys [tx-meta] :as tx-report}]
  (:datastore-connection/bootstrap-schema? tx-meta false))

(defn bootstrap-schema
  [tx-meta bootstrap?]
  (assoc tx-meta :datastore-connection/bootstrap-schema? bootstrap?))

