(ns einherjar.role.initial
  #?(:clj
     (:require
      [datomic-schema.schema :as datomic.schema :refer [schema fields]])))

;; ---- initial ----

(defn norm-map
  [kind]
  (case kind
    :datomic
    #?(:clj  {::v1 {:txes [{:schemas
                            [(schema role
                               (fields
                                [name :string :unique-value]
                                [members :ref :many]))]}]}}
       :cljs nil)

    :datascript
    {}))
