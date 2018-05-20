(ns einherjar.datastore.sync
  (:require
   [taoensso.encore :as encore]
   [einherjar.datastore.connection :as dtst.conn]
   #?@(:clj  [[clojure.spec.alpha :as spec]]
       :cljs [[cljs.spec.alpha :as spec]])))

(defn- ref-attr?
  [datastore-database attr]
  (let [entity     (dtst.conn/entity datastore-database [:db/ident attr])
        value-type #?(:clj  (:db/valueType entity)
                      :cljs (get-in entity [:db/valueType :db/ident]))]
    (= :db.type/ref value-type)))

;; ---- send datoms ----

(defn- xraw-datoms->datoms
  [datastore-database-before datastore-database-after]
  (let [eid->attr (fn [datastore-database eid]
                    (:db/ident (dtst.conn/entity datastore-database eid)))]
    (map (fn [[eid attr-eid value tx-eid added? :as datom]]
           (if (encore/pos-int? attr-eid)
             (let [datastore-database (if added?
                                        datastore-database-after
                                        datastore-database-before)]
               [eid
                (eid->attr datastore-database attr-eid)
                value
                tx-eid
                added?])
             datom)))))

(defn- xdatoms->tx-data
  []
  (map (fn [[eid attr value tx-eid added?]]
         (let [db-fn (if added? :db/add :db/retract)]
           [db-fn eid attr value]))))

(defn- pos-int->lookup-ref
  [datastore-database eid]
  (encore/when-let [correct? (spec/valid? ::dtst.conn/real-eid eid)
                    entity   (dtst.conn/entity datastore-database eid)]
    (or (find entity :db/ident)
        (find entity :db.entity/id))))

(defn- lookup-ref->pos-int
  [datastore-database lookup-ref]
  (encore/when-let [correct? (spec/valid? ::dtst.conn/lookup-ref lookup-ref)
                    eid      (dtst.conn/entid datastore-database lookup-ref)]
    eid))

(defn- translate-eid!
  ([{:keys [eid-mapping_] :as cache} datastore-database eid]
   (encore/when-let [kind           (dtst.conn/kind datastore-database)
                     translated-eid (or (get @eid-mapping_ eid)
                                        (pos-int->lookup-ref datastore-database
                                                             eid)
                                        (lookup-ref->pos-int datastore-database
                                                             eid)
                                        (dtst.conn/tempid kind :db.part/user))]
     (vswap! eid-mapping_ encore/assoc-nx eid translated-eid)
     translated-eid))
  ([{:keys [eid-mapping_ ref-attrs_] :as cache} datastore-database attr eid]
   (if (or (contains? @ref-attrs_ attr)
           (ref-attr? datastore-database attr))
     (do (vswap! ref-attrs_ conj attr)
         (translate-eid! cache datastore-database eid))
     eid)))

(defn- translate-data!
  [cache
   datastore-database-before
   datastore-database-after
   [db-fn eid attr possibly-eid]]
  (let [datastore-database (if (= :db/add db-fn)
                             datastore-database-after
                             datastore-database-before)
        translate          (partial translate-eid! cache datastore-database)]
    [db-fn (translate eid) attr (translate attr possibly-eid)]))

(defn- xtranslate-tx-data
  ([datastore-database-before datastore-database-after]
   (fn [rf]
     (let [cache {:eid-mapping_ (volatile! {})
                  :ref-attrs_   (volatile! #{})}]
       (fn translator
         ([]
          (rf))
         ([container]
          (rf container))
         ([container item]
          (let [new-item (translate-data! cache
                                          datastore-database-before
                                          datastore-database-after
                                          item)]
            (rf container new-item)))))))
  ([datastore-database]
   (xtranslate-tx-data datastore-database datastore-database)))

(defn xdatoms->remote-tx
  [datastore-database-before
   datastore-database-after]
  (comp
   (xraw-datoms->datoms datastore-database-before datastore-database-after)
   (xdatoms->tx-data)
   (xtranslate-tx-data datastore-database-before datastore-database-after)
   (remove #(nil? (second %)))))

;; ---- accept datoms ----

(defn- sort-by-tempid
  [rf]
  (let [old-datas_ (volatile! [])]
    (fn
      ([]
       (rf))
      ([container]
       (rf (reduce rf container @old-datas_)))
      ([container [_ eid :as data]]
       (if (encore/neg-int? eid)
         (rf container data)
         (do (vswap! old-datas_ conj data)
             container))))))

(defn xremote-tx->local-tx
  [datastore-database]
  (cond-> (xtranslate-tx-data datastore-database)
    (= :datascript (dtst.conn/kind datastore-database))
    (comp sort-by-tempid)))

;; ---- initial data ----

#?(:clj
   (defn pull-all-schema
     [datastore-database]
     (when (= :datomic (dtst.conn/kind datastore-database))
       (dtst.conn/q '{:find [[(pull ?eid pattern) ...]]
                      :in [$ pattern]
                      :where [[?eid :db/ident]
                              [(missing? $ ?eid :db/fn)]]}
                    datastore-database
                    ['*]))))

#?(:clj
   (defn pull-all-data
     [datastore-database]
     (when (= :datomic (dtst.conn/kind datastore-database))
       (dtst.conn/q '{:find [[(pull ?eid pattern) ...]]
                      :in [$ pattern]
                      :where [[?eid :db.entity/id]]}
                    datastore-database
                    ['*]))))

;; ---- send pulled data ----

(declare new-datoms)

(defn- map->datoms
  ([m eid]
   (into []
         (mapcat (fn [[attr value]]
                   (new-datoms eid attr value nil true)))
         (dissoc m :db/id)))
  ([m]
   (let [eid (encore/have (:db/id m))]
     (map->datoms m eid))))

(defn- new-datoms
  [eid attr value date added?]
  (encore/cond
    (map? value)
    (let [sub-eid (encore/have (:db/id value))]
      (conj (map->datoms value sub-eid)
            [eid attr sub-eid date added?]))

    (coll? value)
    (into []
          (mapcat #(new-datoms eid attr % date added?))
          value)

    :else [[eid attr value date added?]]))

(defn xpulled-data->remote-tx
  [datastore-database]
  (comp
   (mapcat map->datoms)
   (xdatoms->tx-data)
   (xtranslate-tx-data datastore-database)))
