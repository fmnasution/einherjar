(ns einherjar.router.handler
  (:require
   [einherjar.async.effect :as asnc.efc]
   [einherjar.datastore.connection :as dtst.conn]
   [einherjar.main.datastore :as mn.dtst]
   [einherjar.router.datastore :as rtr.dtst]))

;; ---- event handler ----

#?(:cljs
   (asnc.efc/reg-event
    :html-router/error
    (fn [_ [_ data]]
      [[:logger/error data]])))

#?(:cljs
   (asnc.efc/reg-event
    :html-router/location
    (fn [{:keys [datastore-database]} [_ location]]
      (let [kind    (dtst.conn/kind datastore-database)
            tx-data (-> location
                        (mn.dtst/assoc-nx-eid-id kind :db.part/user)
                        (rtr.dtst/location->tx-data))
            tx-meta {}]
        [[:datastore-connection/transact {:tx-data tx-data
                                          :tx-meta tx-meta}]]))))
