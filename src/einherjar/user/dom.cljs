(ns einherjar.user.dom
  (:require
   [cljs-react-material-ui.rum :as rum.mui]
   [rum.core :as rum :refer [defcs]]
   [com.rpl.specter :as specter :include-macros true]
   [taoensso.encore :as encore]
   [einherjar.element.dom.mixin :as el.dm.mxn]
   [einherjar.element.manager :as el.mng]))

;; ---- login form ----

(defn- login-form
  []
  {:fields    {:user/email    {:type                :text
                               :floating-label-text "Email"}
               :user/password {:type                :text
                               :floating-label-text "Password"}}
   :validator {:user/email    [[encore/nblank-str? "required"]]
               :user/password [[encore/nblank-str? "required"]]}
   :initial   {:user/email    ""
               :user/password ""}
   :on-submit (fn [manager data]
                (let [event [:server-ajax-caller/request-authentication
                             {:credential data}]]
                  (el.mng/dispatch! manager event)))})

(defn- button-disabled?
  [touched error]
  ())

(defcs <login-form> < (el.dm.mxn/form (login-form))
  [state manager]
  (let [{{:keys [fields
                 data
                 touched
                 error
                 update-data
                 update-touched
                 update-error
                 validator
                 on-submit]} ::el.dm.mxn/form} state]
    [:div
     [:form {:on-submit on-submit}
      (for [[k field] fields
            :let      [current-value     (get data k)
                       current-error     (get error k)
                       current-validator (get validator k)]]
        [:div {:key k}
         (rum/with-key
           (rum.mui/text-field
            (assoc field
                   :default-value current-value
                   :error-text    current-error
                   :on-blur       #(encore/do-nil
                                    (let [error (current-validator
                                                 current-value)]
                                      (update-touched assoc k true)
                                      (update-error assoc k error)))
                   :on-change     (fn [e]
                                    (let [value (-> e .-target .-value)]
                                      (encore/do-nil
                                       (update-data assoc k value)
                                       (let [error (current-validator value)]
                                         (update-error assoc k error)))))
                   :on-focus      #(encore/do-nil
                                    (update-touched assoc :first? false))))
           k)])
      [:div
       (rum.mui/raised-button
        {:primary  true
         :type     :submit
         :label    "Login"
         :disabled (or (true? (:first? touched))
                       (->> (dissoc touched :first?)
                            (map val)
                            (some false?)
                            (boolean))
                       (->> error
                            (map val)
                            (some encore/nblank-str?)
                            (boolean)))})]]]))
