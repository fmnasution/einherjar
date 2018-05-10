(ns einherjar.element.react
  (:require
   [goog.dom :as gdom]
   [mount.core :refer [defstate]]
   [rum.core :as rum :refer [defc]]
   [taoensso.timbre :as timbre]
   [einherjar.datastore.connection :as dtst.conn]))

;; ---- rum element ----

;; (defc index-wrapper < rum/reactive
;;   [datastore-connection]
;;   (let [datastore-database (rum/react datastore-connection)]
;;     [:div
;;      [:h1 "Hello World!"]]))

(defc index-wrapper
  [datastore-connection]
  [:div
   [:h1 "Hello World!"]])

(defrecord RumElement [node])

(defn- mount-rum-element!
  [datastore-connection el-id]
  (let [node (gdom/getRequiredElement el-id)]
    (rum/mount (index-wrapper datastore-connection) node)
    (->RumElement node)))

(defn- unmount-rum-element!
  [{:keys [node] :as rum-element}]
  (rum/unmount node))

(defstate rum-element
  :start (do (timbre/info "Mounting rum element...")
             (mount-rum-element! @dtst.conn/datastore-connection "app"))
  :stop  (do (timbre/info "Unmounting rum element...")
             (unmount-rum-element! @rum-element)))
