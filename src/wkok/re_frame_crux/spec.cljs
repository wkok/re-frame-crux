(ns wkok.re-frame-crux.spec
  (:require [clojure.spec.alpha :as s]))

(def event-or-fn? #(or (fn? %)
                       (and (vector? %)
                            (-> % first keyword?))))

(def string-or-keyword? #(or (string? %) (keyword? %)))

(def operation? #(and (vector? %)
                      (some #{(first %)} [:crux.tx/put
                                          :crux.tx/delete
                                          :crux.tx/match
                                          :crux.tx/evict])))

(def effect? #(and (vector? %)
                   (some #{(first %)} [:crux/put
                                       :crux/get
                                       :crux/query
                                       :crux/delete
                                       :crux/submit-tx])))

(s/def ::on-success event-or-fn?)

(s/def ::on-failure event-or-fn?)

(s/def ::valid-time inst?)

(s/def ::end-valid-time inst?)

(s/def ::tx-time inst?)

(s/def ::tx-id string?)

(s/def ::id string-or-keyword?)

(s/def ::doc (s/map-of keyword? any?))

(s/def ::query (s/map-of keyword? any?))

(s/def ::ops (s/coll-of operation?))

(s/def ::put-options (s/keys :req-un [::doc]
                             :opt-un [::on-success
                                      ::on-failure
                                      ::valid-time
                                      ::end-valid-time]))

(s/def ::get-options (s/keys :req-un [::id]
                             :opt-un [::on-success
                                      ::on-failure
                                      ::valid-time
                                      ::tx-time
                                      ::tx-id]))

(s/def ::delete-options (s/keys :req-un [::id]
                                :opt-un [::on-success
                                         ::on-failure
                                         ::valid-time
                                         ::end-valid-time]))

(s/def ::query-options (s/keys :req-un [::query]
                               :opt-un [::on-success
                                        ::on-failure
                                        ::valid-time
                                        ::tx-time
                                        ::tx-id]))

(s/def ::tx-options (s/keys :req-un [::ops]
                            :opt-un [::on-success
                                     ::on-failure]))

(s/def ::multi-options (s/coll-of effect?))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Wrapping clojure.spec.alpha for convenience 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn conform [spec data]
  (let [result (s/conform spec data)]
    (if (= result ::s/invalid)
      (throw (ex-info "Invalid input" (s/explain-data spec data)))
      result)))

