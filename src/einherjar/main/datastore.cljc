(ns einherjar.main.datastore
  (:require
   [datascript.core :as datascript]
   #?@(:clj  [[datomic-schema.schema :refer [schema fields]]]
       :cljs [[goog.crypt.base64 :as base64]]))
  #?(:clj
     (:import
      [java.util Base64])))

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

;; ---- processor ----

(defn- encode-base64
  [input]
  #?(:clj  (.encodeToString (Base64/getEncoder) (.getBytes input))
     :cljs (base64/encodeString input)))

(defn entity-id
  []
  (encode-base64 (str (datascript/squuid))))
