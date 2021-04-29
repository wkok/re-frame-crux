;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Author: Werner Kok (wernerkok@gmail.com)
;;; Copyright (c) 2021, Werner Kok
;;;
;;; Built on ideas from https://github.com/deg/re-frame-firebase
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns wkok.re-frame-crux
  (:require [re-frame.core :as rf]
            [wkok.re-frame-crux.database :as database]))

;; Puts a Document map into Crux
;; See https://opencrux.com/reference/transactions.html#put
;;
;; Key arguments:
;; - :doc             Map corresponding to the document.
;;                    (The :crux.db/id key is optional, if not provided, a UUID will be injected)
;; - :valid-time      Optional. Example: "2017-01-20" See: https://opencrux.com/reference/transactions.html#valid-times
;; - :end-valid-time  Optional. Example: "2017-01-20" See: https://opencrux.com/reference/transactions.html#valid-times
;; - :on-success      Function or re-frame event vector to be dispatched.
;; - :on-failure      Function or re-frame event vector to be dispatched.
;;
;; Example FX:
;; {:crux/put {:doc        {:crux.db/id           :president/biden
;;                          :president/first-name "Joe"
;;                          :president/last-name  "Biden"}
;;             :on-success [:success-event]
;;             :on-failure #(js/console.log %)}}
;;
(rf/reg-fx :crux/put database/put-effect!)


;; Returns the document map for a particular entity
;; See: https://opencrux.com/reference/http.html#entity
;;
;; Key arguments:
;; - :id              Document ID
;; - :valid-time      Optional. Example: "2017-01-20" See: https://opencrux.com/reference/transactions.html#valid-times
;; - :tx-time         Optional. Example: "2017-01-20" See: https://opencrux.com/reference/transactions.html#valid-times
;; - :tx-id           Optional. Transaction ID
;; - :on-success      Function or re-frame event vector to be dispatched.
;; - :on-failure      Function or re-frame event vector to be dispatched.
;;
;; Example FX:
;; {:crux/get {:id         :president/biden
;;             :on-success [:success-event]
;;             :on-failure #(js/console.log %)}}
;;
(rf/reg-fx :crux/get database/get-effect!)


;; Deletes a Document from Crux
;; See https://opencrux.com/reference/transactions.html#delete
;;
;; Key arguments:
;; - :id              Document ID to be deleted
;; - :valid-time      Optional. Example: "2017-01-20" See: https://opencrux.com/reference/transactions.html#valid-times
;; - :end-valid-time  Optional. Example: "2017-01-20" See: https://opencrux.com/reference/transactions.html#valid-times
;; - :on-success      Function or re-frame event vector to be dispatched.
;; - :on-failure      Function or re-frame event vector to be dispatched.
;;
;; Example FX:
;; {:crux/delete {:id         :president/trump
;;                :on-success [:success-event]
;;                :on-failure #(js/console.log %)}}
;;
(rf/reg-fx :crux/delete database/delete-effect!)


;; Takes a datalog query and returns its results
;; See https://opencrux.com/reference/queries.html
;;
;; Key arguments:
;; - :query       Quoted EDN Map containing the Datalog query
;; - :on-success  Function or re-frame event vector to be dispatched.
;; - :on-failure  Function or re-frame event vector to be dispatched.
;;
;; Example FX:
;; {:crux/query {:query      '{:find  [?first-name ?last-name]
;;                             :keys  [first-name last-name]
;;                             :where [[?e :president/first-name ?first-name]
;;                                     [?e :president/last-name ?last-name]]}
;;               :on-success [:success-event]
;;               :on-failure #(js/console.log %)}}
;;
(rf/reg-fx :crux/query database/query-effect)


;; Transactions are comprised of a vector of Transaction Operations to be performed.
;; See https://opencrux.com/reference/transactions.html
;;
;; Key arguments:
;; - :ops         Vector consisting of transaction operations
;; - :on-success  Function or re-frame event vector to be dispatched.
;; - :on-failure  Function or re-frame event vector to be dispatched.
;;
;; Example FX:
;; {:crux/submit-tx {:ops        [[:crux.tx/put {:crux.db/id :president/biden :president/first-name "Joe"}]
;;                                [:crux.tx/delete :president/trump]]
;;                   :on-success [:success-event]
;;                   :on-failure #(js/console.log %)}}
;;
(rf/reg-fx :crux/submit-tx database/submit-tx-effect!)


;; Convenience effect used to dispatch a vector of crux effects.
;; (Not to be confused with Crux transactions)
;;
;; :crux/multi will execute a 'vector of effects', for example,
;; it can be used to execute multiple queries each with separate on-success callbacks
;;
;; Example FX:
;; {:crux/multi [[:crux/query {:query      '{:find  [?first-name ?last-name]
;;                                           :keys  [first-name last-name]
;;                                           :where [[?e :president/first-name ?first-name]
;;                                                   [?e :president/last-name ?last-name]]}
;;                             :on-success [:success-event-president]}]
;;               [:crux/query {:query      '{:find  [?first-name ?last-name]
;;                                           :keys  [first-name last-name]
;;                                           :where [[?e :deputy/first-name ?first-name]
;;                                                   [?e :deputy/last-name ?last-name]]}
;;                             :on-success [:success-event-deputy]}]
;;               [:crux/put {:doc        {:crux.db/id           :secretary/blinken
;;                                        :secretary/first-name "Antony"
;;                                        :secretary/last-name  "Blinken"}
;;                           :on-success [:success-event]}]]}
;;
(rf/reg-fx
  :crux/multi
  (fn [effects]
    (run! (fn [[effect-type args]]
            (case effect-type
              :crux/put       (database/put-effect! args)
              :crux/delete    (database/delete-effect! args)
              :crux/query     (database/query-effect args)
              :crux/submit-tx (database/submit-tx-effect! args)
              (js/console.log "Internal error: unknown crux effect: " effect-type " (" args ")")))
          effects)))


;; Subscription will return the authenticated JWT token and pass it in
;; the Authorization HTTP header when communicating with Crux
;;
;; Your application is responsible for handling authentication with some authentication provider
;; and populate the received JWT token in the re-frame app db under the :crux -> :jwt key
;; like this: {:crux {:jwt "mytoken"}}
;;
;; This is necessary if a JWT key set is configured in the Crux HTTP feature
;; See: https://opencrux.com/reference/http.html#start-http-server
;;
(rf/reg-sub
  :crux/jwt
  (fn [db _]
    (get-in db [:crux :jwt] "")))


;; Subscription will return the URL to which to connect to the Crux REST API
;;
;; Your application is responsible for populating the url in the re-frame app db under the :crux -> :url key
;; like this: {:crux {:url "https://url-to-crux"}}
;;
;; Note this is optional, if not set in the re-frame app-db, ajax requests to Crux will be sent to the same
;; server that hosts the compiled Javascript
;;
(rf/reg-sub
  :crux/url
  (fn [db _]
    (get-in db [:crux :url] "")))
