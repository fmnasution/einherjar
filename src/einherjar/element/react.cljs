(ns einherjar.element.react
  (:require
   [goog.dom :as gdom]
   [mount.core :refer [defstate]]
   [rum.core :as rum :refer [defc]]
   [taoensso.timbre :as timbre]
   [einherjar.datastore.connection :as dtst.conn]
   [einherjar.async.event :as asnc.evt]
   [einherjar.element.manager :as el.mng]))

;; ---- rum element ----

(defc index
  [manager]
  [:div
   [:h1 "Hello World!"]])

(defrecord RumElement [node rum-el])

(defn- mount-rum-element!
  [manager rum-el el-id]
  (let [node (gdom/getRequiredElement el-id)]
    (rum/mount (rum-el manager) node)
    (->RumElement node rum-el)))

(defn- unmount-rum-element!
  [{:keys [node] :as rum-element}]
  (rum/unmount node))

(defstate rum-element
  :start (do (timbre/info "Mounting rum element...")
             (mount-rum-element!
              (el.mng/new-manager @dtst.conn/datastore-connection
                                  @asnc.evt/event-dispatcher)
              index
              "app"))
  :stop  (do (timbre/info "Unmounting rum element...")
             (unmount-rum-element! @rum-element)))

(defn remount!
  [{:keys [node rum-el] :as rum-element} manager]
  (rum/mount (rum-el manager) node))
