(ns einherjar.element.react
  (:require
   [goog.dom :as gdom]
   [mount.core :refer [defstate]]
   [rum.core :as rum :refer [defc]]
   [taoensso.timbre :as timbre]
   [einherjar.datastore.connection :as dtst.conn]
   [einherjar.async.event :as asnc.evt]
   [einherjar.element.manager :as el.mng]
   [einherjar.element.dom.index :as el.dm.idx]))

;; ---- rum element ----

(defrecord ReactElement [node react-el])

(defn- mount-react-element!
  [manager react-el el-id]
  (let [node (gdom/getRequiredElement el-id)]
    (rum/mount (react-el manager) node)
    (->ReactElement node react-el)))

(defn- unmount-react-element!
  [{:keys [node] :as react-element}]
  (rum/unmount node))

(defstate react-element
  :start (do (timbre/info "Mounting react element...")
             (mount-react-element!
              (el.mng/new-manager @dtst.conn/datastore-connection
                                  @asnc.evt/event-dispatcher)
              el.dm.idx/<index>
              "app"))
  :stop  (do (timbre/info "Unmounting react element...")
             (unmount-react-element! @react-element)))

(defn remount!
  [{:keys [node react-el] :as react-element} manager]
  (rum/mount (react-el manager) node))
