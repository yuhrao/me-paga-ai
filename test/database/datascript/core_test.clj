(ns database.datascript.core-test
  (:require [database.datascript.core :as mem-db]
            [database.core :as db]
            [matcher-combinators.test]
            [matcher-combinators.matchers :as matchers]
            [clojure.test :refer [deftest testing is are]]
            [tick.core :as tick]
            [datascript.core :as ds]))

(def valid-transaction {:transaction/amount (bigdec 12)
                        :transaction/method :credit-card
                        :transaction/card {:card/number "1234123412341234"
                               :card/holder-name "Maijon Djékison"
                               :card/security-code 123
                               :card/due-date (tick/date-time)}})

(defn get-all-transactions [database]
  (let [{:keys [conn]} database]
    (->> (ds/q '[:find (pull ?e [*])
                 :where [?e :transaction/amount]]
               @conn)
         (mapcat identity))))

(defn fetch-transaction [database id]
  (ds/pull (ds/db (:conn database)) '[*] id))

(deftest create-transaction!
  (testing "Creating one transaction"
    (let [database (mem-db/->MemoryDatabase {})
          id       1
          new-tx   (assoc valid-transaction :db/id 1)]
      (db/create-transaction! database new-tx)
      (is (match? (matchers/equals new-tx)
                  (fetch-transaction database id))
          "persisted entity should match values and fields")
      (is (match? 1
                  (count (get-all-transactions database)))
          "should have only one transaction persisted")))
  (testing "Creating lots of transactions"
    (let [database (mem-db/->MemoryDatabase {})
          new-txs  (->> [(assoc valid-transaction :amount (bigdec 1))
                         (assoc valid-transaction :amount (bigdec 2))]
                        (map-indexed #(assoc %2 :db/id (inc %1))))]
      (doseq [tx new-txs]
        (db/create-transaction! database tx))

      (are [tx id]
          (match? (matchers/equals tx)
                  (fetch-transaction database id))
        (nth new-txs 0) 1
        (nth new-txs 1) 2)

      (is (match? 2
                  (count (get-all-transactions database)))))))
