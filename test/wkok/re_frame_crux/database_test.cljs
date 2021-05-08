(ns wkok.re-frame-crux.database-test
  (:require [cljs.test :refer (deftest testing is)]
            [re-frame.core :as rf]
            [day8.re-frame.test :as rf-test]
            [wkok.re-frame-crux]
            [wkok.re-frame-crux.database :as database]
            [ajax.edn :refer [edn-request-format]]))

(def on-success (fn [] true))
(def on-failure (fn [] false))

(deftest set-headers-test
  (testing "Test that request headers gets set, in particular, the JWT token
should be picked up from a subscription and added to the authorization header"
    (rf-test/run-test-sync
      (rf/dispatch [:initialize-test-db {:crux {:jwt "mytoken"}}])
      (is (= (database/set-headers)
             {"authorization" "Bearer mytoken"})))))

(deftest event->fn-test
  (testing "Test that a re-frame event vector gets converted to a function using fn"
    (is (fn? (database/event->fn [:some-event])))))

(deftest to-cljs-ajax-test
  (testing "Test that the required keys are present in the cljs-ajax map"
    (let [options {:params     {:some "stuff"}
                   :on-success on-success
                   :on-failure on-failure}]
      (is (= {:format        (edn-request-format)
              :params        {:some "stuff"}
              :headers       {}
              :handler       on-success
              :error-handler on-failure}
             (database/to-cljs-ajax options))))))

(deftest crux-url-test
  (testing "Test that the Crux url includes the host when set in app-db"
    (is (= "/_crux/submit-tx"
           (database/crux-url "/_crux/submit-tx")))
    (rf-test/run-test-sync
      (rf/dispatch [:initialize-test-db {:crux {:url "https://crux-host"}}])
      (is (= "https://crux-host/_crux/submit-tx"
             (database/crux-url "/_crux/submit-tx"))))))

(deftest submit-tx-payload-test
  (testing "Test that options get converted to valid crux payload for /submit-tx"
    (let [options {:ops        [[:crux.tx/put {:crux.db/id :president/biden :president/first-name "Joe"}]
                                [:crux.tx/delete :president/trump]]
                   :on-success on-success
                   :on-failure on-failure}]
      (is (= {:format        (edn-request-format)
              :params        {:tx-ops (:ops options)}
              :headers       {}
              :handler       on-success
              :error-handler on-failure}
             (database/submit-tx-payload options))))))

(deftest query-payload-test
  (testing "Test that options get converted to valid crux payload for /query"
    (let [options {:query      '{:find  [?first-name ?last-name]
                                 :keys  [first-name last-name]
                                 :where [[?e :president/first-name ?first-name]
                                         [?e :president/last-name ?last-name]]}
                   :on-success on-success
                   :on-failure on-failure}]
      (is (= {:format        (edn-request-format)
              :params        {:query (:query options)}
              :headers       {}
              :handler       on-success
              :error-handler on-failure}
             (database/query-payload options))))))

(deftest make-query-url-test
  (testing "Test that #inst gets correctly formatted to ISOString"
    (is (= "/_crux/query?valid-time=2020-01-22T00:00:00.000Z&"
           (database/make-query-url {:valid-time #inst "2020-01-22"})))))

(deftest with-crux-id-test
  (testing "Test that the required :crux.db/id key is generated if not specified"
    (is (contains? (database/with-crux-id {:president/first-name "Joe"
                                           :president/last-name  "Biden"})
                   :crux.db/id))))

(deftest make-put-op-test
  (testing "Test that a document gets converted to a Crux put operation"
    (let [document {:president/first-name "Joe"
                    :president/last-name  "Biden"}]
      (is (= [:crux.tx/put document]
             (database/make-put-op {} document))))))

(deftest make-delete-op-test
  (testing "Test that a document gets converted to a Crux delete operation"
    (is (= [:crux.tx/delete :president/trump]
           (database/make-delete-op {:id :president/trump})))))

(deftest make-entity-url-test
  (testing "Test that options get converted to a valid Crux /entity URL when the ID is specified as either a keyword or string or uuid"
    (is (= "/_crux/entity?eid-edn=:president/trump"
           (database/make-entity-url {:id :president/trump})))
    (is (= "/_crux/entity?eid=trump"
           (database/make-entity-url {:id "trump"})))
    (is (= "/_crux/entity?eid-edn=%23uuid \"94f8f299-9fbb-4b9e-a570-847d31f93447\""
           (database/make-entity-url {:id #uuid "94f8f299-9fbb-4b9e-a570-847d31f93447"})))))

(rf/reg-event-fx
  :initialize-test-db
  (fn [_ [_ db]]
    {:db db}))
