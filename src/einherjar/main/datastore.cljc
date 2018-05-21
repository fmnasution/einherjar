(ns einherjar.main.datastore
  (:require
   [taoensso.encore :as encore]
   [datascript.core :as datascript]
   [einherjar.datastore.connection :as dtst.conn]
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

(defn tempid
  ([kind prtn n]
   (dtst.conn/tempid kind prtn n))
  ([kind prtn]
   (dtst.conn/tempid kind prtn)))

(defn- -assoc-nx-eid-id
  [m temp-eid entity-id]
  (encore/assoc-nx m
                   :db/id        temp-eid
                   :db.entity/id entity-id))

(defn assoc-nx-eid-id
  ([m kind prtn n]
   (-assoc-nx-eid-id m (tempid kind prtn n) (entity-id)))
  ([m kind prtn]
   (-assoc-nx-eid-id m (tempid kind prtn) (entity-id))))

(defmulti update-data
  (fn [kind [db-fn eid attr value]]
    [db-fn attr]))

(defmethod update-data :default
  [kind data]
  data)
