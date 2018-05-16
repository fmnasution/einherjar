(ns einherjar.datastore.handler
  (:require
   [einherjar.async.effect :as asnc.efc]
   [einherjar.datastore.connection :as dtst.conn]))

;; ---- effect handler ----

(asnc.efc/reg-effect
 :datastore-connection/transact
 (fn [{:keys [datastore-connection]} [_ {:keys [tx-data tx-meta]}]]
   (if (map? tx-meta)
     (dtst.conn/transact datastore-connection tx-data tx-meta)
     (dtst.conn/transact datastore-connection tx-data))))
