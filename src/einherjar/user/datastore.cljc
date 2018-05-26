(ns einherjar.user.datastore
  (:require
   [taoensso.encore :as encore]
   [einherjar.main.datastore :as mn.dtst]
   #?@(:clj [[buddy.hashers :as buddy.hash]
             [datomic-schema.schema :refer [schema fields]]])))

(declare user->tx-data)

;; ---- initial ----

(defn norm-map
  [kind]
  (case kind
    :datomic
    #?(:clj  {::v1 {:txes     [{:schemas
                                [(schema user
                                   (fields
                                    [name :string]
                                    [password :string]
                                    [emails :string :many :unique-value]))]}
                               {:datas
                                (-> {:user/name     "admin"
                                     :user/password "admin"
                                     :user/emails   ["admin@einherjar.io"]}
                                    (mn.dtst/assoc-nx-eid-id kind :db.part/user)
                                    (user->tx-data))}]
                    :requires [:einherjar.main.datastore/v1]}}
       :cljs nil)

    :datascript
    {}))

;; ---- processor ----

(defn user->tx-data
  [{eid       :db/id
    entity-id :db.entity/id
    uname     :user/name
    upassword :user/password
    uemails   :user/emails}]
  (-> []
      (encore/conj-when
       (when entity-id
         [:db/add eid :db.entity/id entity-id])
       (when uname
         [:db/add eid :user/name uname])
       (when upassword
         [:db/add eid :user/password upassword]))
      (into (map (fn [uemail]
                   [:db/add eid :user/emails uemail])) uemails)))

#?(:clj
   (defmethod mn.dtst/update-data [:db/add :user/password]
     [kind [db-fn eid attr value]]
     [db-fn eid attr (buddy.hash/derive value)]))
