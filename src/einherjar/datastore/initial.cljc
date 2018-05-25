(ns einherjar.datastore.initial
  (:require
   [mount.core :refer [defstate]]
   [com.rpl.specter :as specter :include-macros true]
   [taoensso.timbre :as timbre]
   [taoensso.encore :as encore]
   [einherjar.datastore.connection :as dtst.conn]
   [einherjar.main.datastore :as mn.dtst]
   [einherjar.user.datastore :as usr.dtst]
   [einherjar.role.datastore :as rl.dtst]
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
     (specter/transform [specter/MAP-VALS (specter/must :txes)]
                        #(into [] (map init-config->tx-data) %)
                        norm-map)))

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
                (encore/merge (mn.dtst/norm-map kind)
                              (usr.dtst/norm-map kind)
                              (rl.dtst/norm-map kind))))))

;; ---- spec ----

(spec/def ::schemas
  (spec/cat :schemas (spec/+ map?)))

(spec/def ::datas
  (spec/cat :datas (spec/+ (spec/or :map-form map? :vector-form vector?))))

(spec/def ::init-config
  (spec/and (spec/keys :opt-un [::schemas ::datas]) seq))

(spec/def ::txes
  (spec/coll-of ::init-config))

(spec/def ::txes-fn
  encore/qualified-symbol?)

(spec/def ::requires
  (spec/cat :keywords (spec/+ encore/qualified-keyword?)))

(spec/def ::norm
  (spec/and (spec/keys :opt-un [::txes ::txes-fn ::requires])
            (fn [m]
              (or (contains? m :txes)
                  (contains? m :txes-fn)))))

(spec/def ::norm-map
  (spec/map-of encore/qualified-keyword? ::norm))
