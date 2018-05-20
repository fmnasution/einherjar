(ns einherjar.datastore.initial
  (:require
   [mount.core :refer [defstate]]
   [taoensso.timbre :as timbre]
   [taoensso.encore :as encore]
   [einherjar.datastore.connection :as dtst.conn]
   [einherjar.user.initial :as usr.init]
   [einherjar.role.initial :as rl.init]
   #?@(:clj  [[clojure.spec.alpha :as spec]
              [datomic-schema.schema :as datomic.schema :refer [schema fields]]
              [io.rkn.conformity :as datomic.conform]]
       :cljs [[cljs.spec.alpha :as spec]])))

;; ---- bootstrap ----

#?(:clj
   (defn- init-config->tx-data
     [{:keys [schemas datas] :as init-config}]
     (encore/into-all []
                      (datomic.schema/generate-schema schemas {:index-all? true
                                                               :gen-all?   true})
                      datas)))

#?(:clj
   (defn- process-norm-map
     [norm-map]
     (encore/map-vals
      (fn [conformable]
        (if (contains? conformable :txes)
          (update conformable :txes #(into [] (map init-config->tx-data) %))
          conformable))
      norm-map)))

(defn- norm-map
  [kind]
  (case kind
    :datomic
    #?(:clj  {::v1 {:txes [{:schemas
                            [(schema db.entity
                               (fields
                                [id :string :unique-identity]
                                [created-at :instant]
                                [created-by :ref]))]}]}}
       :cljs nil)

    :datascript
    {}))

(defn- bootstrap-datastore!
  [datastore-connection norm-map]
  (case (dtst.conn/kind datastore-connection)
    :datomic
    #?(:clj  (datomic.conform/ensure-conforms
              (dtst.conn/internal datastore-connection)
              (process-norm-map (spec/assert ::norm-map norm-map)))
       :cljs nil)
    :datascript {}))

(defstate datastore-bootstrapper
  :start (do (timbre/info "Bootstrappting datastore...")
             (bootstrap-datastore!
              @dtst.conn/datastore-connection
              (let [kind (dtst.conn/kind @dtst.conn/datastore-connection)]
                (encore/merge (norm-map kind)
                              (usr.init/norm-map kind)
                              (rl.init/norm-map kind))))))

;; ---- spec ----

(spec/def ::schemas
  (spec/and (spec/coll-of map?) seq))

(spec/def ::datas
  (spec/and (spec/coll-of (spec/or :map-form map? :vector-form vector?)) seq))

(spec/def ::init-config
  (spec/and (spec/keys :opt-un [::schemas ::datas]) seq))

(spec/def ::txes
  (spec/coll-of ::init-config))

(spec/def ::txes-fn
  encore/qualified-symbol?)

(spec/def ::requires
  (spec/and (spec/coll-of keyword?) seq))

(spec/def ::norm
  (spec/and (spec/keys :opt-un [::txes ::txes-fn ::requires])
            (fn [m]
              (or (contains? m :txes)
                  (contains? m :txes-fn)))))

(spec/def ::norm-map
  (spec/map-of encore/qualified-keyword? ::norm))
