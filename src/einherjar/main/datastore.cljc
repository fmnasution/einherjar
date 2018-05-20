(ns einherjar.main.datastore
  #?(:clj
     (:require
      [datomic-schema.schema :as datomic.schema :refer [schema fields]])))

;; ---- initial ----

(defn norm-map
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
