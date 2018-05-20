(ns einherjar.element.handler
  (:require
   [einherjar.async.effect :as asnc.efc]
   [einherjar.element.manager :as el.mng]
   [einherjar.element.react :as el.rct]
   [einherjar.datastore.connection :as dtst.conn]))

;; ---- effect handler ----

(asnc.efc/reg-effect
 :react-element/remount
 (fn [{:keys [react-element event-dispatcher]} [_ {:keys [db-after]}]]
   (let [manager (el.mng/new-manager
                  (dtst.conn/new-datastore-database db-after)
                  event-dispatcher)]
     (el.rct/remount! react-element manager))))
