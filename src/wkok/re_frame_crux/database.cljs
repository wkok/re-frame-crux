(ns wkok.re-frame-crux.database
  (:require [ajax.core :refer [GET POST]]
            [ajax.edn :refer [edn-request-format]]
            [re-frame.core :as rf]))

(defn set-headers
  "Sets HTTP request headers"
  []
  (let [jwt @(rf/subscribe [:crux/jwt])]
    (-> {}
        (cond-> jwt (assoc "authorization" (str "Bearer " jwt))))))

(defn event->fn
  "Converts a re-frame event vector to a function that can be passed as cljs-ajax callbacks"
  [event-fn]
  (if (vector? event-fn)
    #(rf/dispatch (conj event-fn %))
    event-fn))

(defn to-cljs-ajax
  "Wraps the params in a Map that cljs-ajax understand"
  [{:keys [params on-success on-failure]}]
  {:format        (edn-request-format)
   :params        params
   :headers       (set-headers)
   :handler       (or (event->fn on-success) js/console.log)
   :error-handler (or (event->fn on-failure) js/console.log)})

(defn submit-tx-effect!
  "See the doc for the :crux/submit-tx effect in the re-frame-crux namespace"
  [{:keys [ops] :as options}]
  (POST (str @(rf/subscribe [:crux/url]) "/_crux/submit-tx")
        (-> (assoc options :params {:tx-ops ops})
            to-cljs-ajax)))

(defn query-effect
  "See the doc for the :crux/query effect in the re-frame-crux namespace"
  [{:keys [query valid-time tx-time tx-id] :as options}]
  (POST (str @(rf/subscribe [:crux/url]) "/_crux/query")
        (-> (assoc options :params (-> {}
                                       (cond-> query      (assoc :query query)
                                               valid-time (assoc :valid-time valid-time)
                                               tx-time    (assoc :tx-time tx-time)
                                               tx-id      (assoc :tx-id tx-id))))
            to-cljs-ajax)))

(defn put-effect!
  "See the doc for the :crux/put effect in the re-frame-crux namespace"
  [{:keys [doc valid-time end-valid-time] :as options}]
  (let [doc (if-not (:crux.db/id doc)
              (assoc doc :crux.db/id (random-uuid))
              doc)
        op  (-> [:crux.tx/put doc]
                (cond-> valid-time (conj valid-time)
                        end-valid-time (conj end-valid-time)))]
    (-> (assoc options :ops [op])
        submit-tx-effect!)))

(defn delete-effect!
  "See the doc for the :crux/delete effect in the re-frame-crux namespace"
  [{:keys [id valid-time end-valid-time] :as options}]
  (let [op (-> [:crux.tx/delete id]
               (cond-> valid-time (conj valid-time)
                       end-valid-time (conj end-valid-time)))]
    (-> (assoc options :ops [op])
        submit-tx-effect!)))

(defn get-effect!
  "See the doc for the :crux/get effect in the re-frame-crux namespace"
  [{:keys [id valid-time tx-time tx-id on-success on-failure]}]
  (GET (str @(rf/subscribe [:crux/url]) "/_crux/entity"
            (if (keyword? id) "?eid-edn=" "?eid=")
            id
            (when valid-time (str "&valid-time=" valid-time))
            (when tx-time (str "&tx-time=" tx-time))
            (when tx-id (str "&tx-id=" tx-id)))
       {:handler       (or (event->fn on-success) js/console.log)
        :error-handler (or (event->fn on-failure) js/console.log)}))

