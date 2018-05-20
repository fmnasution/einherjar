(ns einherjar.role.datastore
  #?(:clj
     (:require
      [datomic-schema.schema :refer [schema fields]])))

;; ---- initial ----

(defn norm-map
  [kind]
  (case kind
    :datomic
    #?(:clj  {::v1 {:txes [{:schemas
                            [(schema role
                               (fields
                                [name :string :unique-value]
                                [members :ref :many]))]}]
                    :requires [:einherjar.user.initial/v1]}}
       :cljs nil)

    :datascript
    {}))
