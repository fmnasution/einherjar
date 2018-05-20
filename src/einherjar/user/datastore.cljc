(ns einherjar.user.datastore
  #?(:clj
     (:require
      [datomic-schema.schema :as datomic.schema :refer [schema fields]])))

;; ---- initial ----

(defn norm-map
  [kind]
  (case kind
    :datomic
    #?(:clj  {::v1 {:txes [{:schemas
                            [(schema user
                               (fields
                                [name :string]
                                [password :string]
                                [emails :string :many :unique-value]))]}]
                    :requires [:einherjar.main.initial/v1]}}
       :cljs nil)

    :datascript
    {}))
