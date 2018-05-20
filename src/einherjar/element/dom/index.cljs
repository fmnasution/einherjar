(ns einherjar.element.dom.index
  (:require
   [rum.core :refer [defc]]
   [einherjar.element.manager :as el.mng]
   [einherjar.datastore.connection :as dtst.conn]))

;; ---- sub ----

(el.mng/reg-sub
 ::all-schema
 (fn [datastore-database _ {:keys [pattern]
                           :or   {pattern ['*]}}]
   (dtst.conn/q '{:find  [[(pull ?eid pattern) ...]]
                  :in    [$ pattern]
                  :where [[?eid :db/ident]]}
                datastore-database
                pattern)))

(el.mng/reg-sub
 ::all-data
 (fn [datastore-database _ {:keys [pattern]
                           :or   {pattern ['*]}}]
   (dtst.conn/q '{:find  [[(pull ?eid pattern) ...]]
                  :in    [$ pattern]
                  :where [[?eid :db.entity/id]]}
                datastore-database
                pattern)))

;; ---- dom ----

(defc display-map
  [m]
  [:div
   [:ul
    (for [[k v] m
          :let [str-k (str k)]]
      [:li {:key str-k}
       [:p (str str-k " == " v)]])]])

(defc display-all-schema
  [manager]
  (if-let [schemas (not-empty (el.mng/subscribe manager ::all-schema {}))]
    [:div
     [:ul
      (for [schema schemas
            :let [eid (:db/id schema)]]
        [:li {:key eid}
         (display-map schema)])]]
    [:div
     [:h1 "No schemas to be displayed!"]]))

(defc display-all-data
  [manager]
  (if-let [datas (not-empty (el.mng/subscribe manager ::all-data {}))]
    [:div
     [:ul
      (for [data datas
            :let [ident (:db.entity/id data)]]
        [:li {:key ident}
         (display-map data)])]]
    [:div
     [:h1 "No datas to be displayed!"]]))

(defc index
  [manager]
  [:div
   [:h1 "Hello World!"]
   (display-all-schema manager)
   (display-all-data manager)
   (display-map {:user/name "faris"
                 :user/email "foobar@foo.bar"})])
