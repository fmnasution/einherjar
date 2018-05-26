(ns einherjar.router.datastore
  (:require
   [taoensso.encore :as encore]
   [einherjar.main.datastore :as mn.dtst]))

;; ---- processor ----

(defn location->tx-data
  [{eid :db/id
    entity-id :db.entity/id
    lhandler :location/handler
    lroute-params :location/route-params}]
  (encore/conj-when
   [[:db/add eid :websocket-remote/client? true]]
   (when entity-id
     [:db/add eid :db.entity/id entity-id])
   (when lhandler
     [:db/add eid :location/handler lhandler])
   (when lroute-params
     [:db/add eid :location/route-params lroute-params])))
