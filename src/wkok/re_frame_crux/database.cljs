(ns wkok.re-frame-crux.database
  (:require [ajax.core :refer [GET POST]]
            [ajax.edn :refer [edn-request-format]]
            [re-frame.core :as rf]
            [clojure.string :as str]))

(defn set-headers
  "Sets HTTP request headers"
  []
  (let [jwt @(rf/subscribe [:crux/jwt])]
    (-> {}
        (cond-> (not (str/blank? jwt)) (assoc "authorization" (str "Bearer " jwt))))))

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

(defn crux-url [path]
  (str @(rf/subscribe [:crux/url]) path))

(defn submit-tx-payload
  [{:keys [ops] :as options}]
  (-> (assoc options :params {:tx-ops ops})
      to-cljs-ajax))

(defn submit-tx-effect!
  "See the doc for the :crux/submit-tx effect in the re-frame-crux namespace"
  [options]
  (POST (crux-url "/_crux/submit-tx")
        (submit-tx-payload options)))

(defn query-payload
  [{:keys [query valid-time tx-time tx-id] :as options}]
  (-> (assoc options :params (-> {}
                                 (cond-> query      (assoc :query query)
                                         valid-time (assoc :valid-time valid-time)
                                         tx-time    (assoc :tx-time tx-time)
                                         tx-id      (assoc :tx-id tx-id))))
      to-cljs-ajax))

(defn query-effect
  "See the doc for the :crux/query effect in the re-frame-crux namespace"
  [options]
  (POST (crux-url "/_crux/query")
        (query-payload options)))

(defn with-crux-id
  [{:keys [doc]}]
  (if-not (:crux.db/id doc)
    (assoc doc :crux.db/id (random-uuid))
    doc))

(defn make-put-op
  [{:keys [valid-time end-valid-time]} document]
  (-> [:crux.tx/put document]
      (cond-> valid-time (conj valid-time)
              end-valid-time (conj end-valid-time))))

(defn tx-payload
  [options op]
  (assoc options :ops [op]))

(defn put-effect!
  "See the doc for the :crux/put effect in the re-frame-crux namespace"
  [options]
  (->> (with-crux-id options)
       (make-put-op options)
       (tx-payload options)
       submit-tx-effect!))

(defn make-delete-op
  [{:keys [id valid-time end-valid-time]}]
  (-> [:crux.tx/delete id]
      (cond-> valid-time (conj valid-time)
              end-valid-time (conj end-valid-time))))

(defn delete-effect!
  "See the doc for the :crux/delete effect in the re-frame-crux namespace"
  [options]
  (-> (make-delete-op options)
      (tx-payload options)
      submit-tx-effect!))

(defn make-entity-url
  [{:keys [id valid-time tx-time tx-id]}]
  (str  (crux-url "/_crux/entity")
        (if (keyword? id) "?eid-edn=" "?eid=")
        id
        (when valid-time (str "&valid-time=" valid-time))
        (when tx-time (str "&tx-time=" tx-time))
        (when tx-id (str "&tx-id=" tx-id))))

(defn get-effect!
  "See the doc for the :crux/get effect in the re-frame-crux namespace"
  [{:keys [on-success on-failure] :as options}]
  (GET (make-entity-url options)
       {:handler       (or (event->fn on-success) js/console.log)
        :error-handler (or (event->fn on-failure) js/console.log)}))

