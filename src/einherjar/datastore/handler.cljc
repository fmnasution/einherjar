(ns einherjar.datastore.handler
  (:require
   [taoensso.encore :as encore]
   [einherjar.async.effect :as asnc.efc]
   [einherjar.datastore.connection :as dtst.conn]
   [einherjar.datastore.sync :as dtst.snc]))

(defn- should-sync?
  [{:keys [tx-meta] :as tx-report}]
  (:datastore-connection/sync? tx-meta true))

(defn- mark-as-should-sync
  [tx-meta]
  (assoc tx-meta :datastore-connection/sync? #?(:clj  true
                                                :cljs false)))

(defn- bootstrap-schema?
  [{:keys [tx-meta] :as tx-report}]
  (:datastore-connection/bootstrap-schema? tx-meta false))

(defn- mark-as-bootstrap-schema?
  [tx-meta]
  (assoc tx-meta :datastore-connection/bootstrap-schema? true))

;; ---- event handler ----

(asnc.efc/reg-event
 :datastore-tx-pipeliner/error
 (fn [_ [_ data]]
   [[:logger/error data]]))

(asnc.efc/reg-event
 :datastore-connection/incoming-remote-tx
 (fn [{:keys [datastore-database]}
     [_ {{:keys [tx-data tx-meta]} :websocket/?data}]]
   (let [tx-data (into []
                       (dtst.snc/xremote-tx->local-tx datastore-database)
                       tx-data)]
     (encore/conj-when []
                       (when (seq tx-data)
                         [:datastore-connection/transact {:tx-data tx-data
                                                          :tx-meta tx-meta}])))))

(asnc.efc/reg-event
 :datastore-connection/tx-report
 (fn [_ [_ tx-report]]
   (encore/conj-when []
                     (when (should-sync? tx-report)
                       [:datastore-connection/sync-tx-report tx-report])
                     (when (bootstrap-schema? tx-report)
                       [:datastore-connection/request-data])
                     #?(:cljs [:rum-element/remount]))))

(asnc.efc/reg-event
 :datastore-connection/sync-tx-report
 (fn [_ [_ {:keys [db-before db-after tx-data tx-meta] :as tx-report}]]
   (let [tx-data (into []
                       (dtst.snc/xdatoms->remote-tx db-before db-after)
                       tx-data)
         tx-meta (mark-as-should-sync tx-meta)
         event   [:datastore-connection/incoming-remote-tx {:tx-data tx-data
                                                            :tx-meta tx-meta}]]
     [[#?(:clj  :websocket-server/publish
          :cljs :websocket-client/publish) {:event event}]])))

#?(:cljs
   (asnc.efc/reg-event
    :datastore-connection/request-schema
    (fn [{:keys [config]} _]
      (let [event [:datastore-connection/send-schema]
            more  [(get-in config [:websocket-client :timeout-ms])
                   [:datastore-connection/transact-remote-tx]
                   [:event-dispatcher/no-op]]]
        [[#?(:clj  :websocket-server/publish
             :cljs :websocket-client/publish) {:event event
                                               :more  more}]]))))

#?(:cljs
   (asnc.efc/reg-event
    :datastore-connection/request-data
    (fn [{:keys [config]} _]
      (let [event [:datastore-connection/send-data]
            more  [(get-in config [:websocket-client :timeout-ms])
                   [:datastore-connection/transact-remote-tx]
                   [:event-dispatcher/no-op]]]
        [[#?(:clj  :websocket-server/publish
             :cljs :websocket-client/publish) {:event event
                                               :more  more}]]))))

#?(:clj
   (asnc.efc/reg-event
    :datastore-connection/send-schema
    (fn [{:keys [datastore-database]} [{:keys [websocket/?reply-fn]}]]
      (let [tx-data (into []
                          (dtst.snc/xpulled-data->remote-tx datastore-database)
                          (dtst.snc/pull-all-schema datastore-database))
            tx-meta (-> {}
                        (mark-as-should-sync)
                        (mark-as-bootstrap-schema?))]
        {:ws-remote/publish {:?reply-fn   ?reply-fn
                             :?reply-data {:tx-data tx-data
                                           :tx-meta tx-meta}}}))))

#?(:clj
   (asnc.efc/reg-event
    :datastore-connection/send-data
    (fn [{:keys [datastore-database]} [{:keys [websocket/?reply-fn]}]]
      (let [tx-data (into []
                          (dtst.snc/xpulled-data->remote-tx datastore-database)
                          (dtst.snc/pull-all-data datastore-database))
            tx-meta (-> {}
                        (mark-as-should-sync)
                        (mark-as-bootstrap-schema?))]
        {:ws-remote/publish {:?reply-fn   ?reply-fn
                             :?reply-data {:tx-data tx-data
                                           :tx-meta tx-meta}}}))))

;; ---- effect handler ----

(asnc.efc/reg-effect
 :datastore-connection/transact
 (fn [{:keys [datastore-connection]} [_ {:keys [tx-data tx-meta]}]]
   (if (map? tx-meta)
     (dtst.conn/transact datastore-connection tx-data tx-meta)
     (dtst.conn/transact datastore-connection tx-data))))
