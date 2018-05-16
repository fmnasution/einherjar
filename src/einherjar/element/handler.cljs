(ns einherjar.element.handler
  (:require
   [einherjar.async.effect :as asnc.efc]
   [einherjar.element.manager :as el.mng]
   [einherjar.element.react :as el.rct]
   [einherjar.datastore.connection :as dtst.conn]))

;; ---- effect handler ----

(asnc.efc/reg-effect
 :rum-element/remount
 (fn [{:keys [rum-element event-dispatcher]} [_ {:keys [db-after]}]]
   (let [manager (el.mng/new-manager
                  (dtst.conn/new-datastore-database db-after)
                  event-dispatcher)]
     (el.rct/remount! rum-element manager))))
