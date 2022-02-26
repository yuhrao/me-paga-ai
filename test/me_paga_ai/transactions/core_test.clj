(ns me-paga-ai.transactions.core-test
  (:require [me-paga-ai.transactions.core :as transaction]
            [tick.core :as tick]
            [matcher-combinators.test]
            [matcher-combinators.matchers :as matchers]
            [medley.core :as medley]
            [clojure.test :refer [deftest testing are is]]
            [database.datascript.core :as mem-db]
            [database.core :as db]))

(defn create-context []
  {:repo (mem-db/->MemoryDatabase)})

(def valid-transaction
  {:amount (bigdec 12)
   :method :credit-card
   :card {:number "1234123412341234"
          :holder-name "Maijon Djékison"
          :security-code 123
          :due-date (tick/date-time)}})

(deftest validate
  (testing "Return error when receiving invalid transaction data with missing fields"
    (are [missing-field]
        (match? {:error {:message "Invalid transaction."
                         :data map?}}
                (-> valid-transaction
                    (medley/dissoc-in missing-field)
                    (transaction/validate)))
      [:amount]
      [:method]
      [:card :number]
      [:card :holder-name]
      [:card :security-code]
      [:card :due-date]))

  (testing "Return error when receiving transacion with invalid amount"
    (are [invalid-value]
        (match? {:error
                 {:message "Invalid transaction."
                  :data {:amount ["should be a decimal"]}}}
                (-> valid-transaction
                    (assoc :amount invalid-value)
                    (transaction/validate)))
      12
      "12"))

  (testing "Return error when receiving transacion with invalid method"
    (are [invalid-value]
        (match? {:error
                 {:message "Invalid transaction."
                  :data {:method ["should be either :debit-card or :credit-card"]}}}
                (-> valid-transaction
                    (assoc :method invalid-value)
                    (transaction/validate)))
      1
      "credit"
      ""
      :ay))

  (testing "Return error when receiving transacion with invalid card number"
    (let [error-kind->message {:length "should be 16 characters"
                               :type   "should be a string"}]
      (are [invalid-value error-kind]
          (match? {:error
                   {:message "Invalid transaction."
                    :data {:card
                           {:number
                            [(error-kind error-kind->message)]}}}}
                  (-> valid-transaction
                      (assoc-in [:card :number] invalid-value)
                      (transaction/validate)))
        1                   :type
        :any                :type
        "12341234123412341" :length
        "123"               :length
        ""                  :length)))

  (testing "Return error when receiving transacion with invalid card holder name"
    (let [error-kind->message {:length "should be at least 3 characters"
                               :type   "should be a string"}]
      (are [invalid-value error-kind]
          (match? {:error
                   {:message "Invalid transaction."
                    :data {:card
                           {:holder-name
                            [(error-kind error-kind->message)]}}}}
                  (-> valid-transaction
                      (assoc-in [:card :holder-name] invalid-value)
                      (transaction/validate)))
        1    :type
        :any :type
        ""   :length
        "a"  :length
        "as" :length)))

  (testing "Return error when receiving transacion with invalid security card"
    (let [error-kind->message {:length "should be between 100 and 999"
                               :type   "should be an integer"}]
      (are [invalid-value error-kind]
          (match? {:error
                   {:message "Invalid transaction."
                    :data {:card
                           {:security-code
                            [(error-kind error-kind->message)]}}}}
                  (-> valid-transaction
                      (assoc-in [:card :security-code] invalid-value)
                      (transaction/validate)))
        1    :length
        10   :length
        :any :type
        ""   :type
        "a"  :type
        "as" :type)))

  (testing "Return error when receiving transacion with invalid card due date"
    (let [error-kind->message {:type "should be a date time"}]
      (are [invalid-value error-kind]
          (match? {:error
                   {:message "Invalid transaction."
                    :data {:card
                           {:due-date
                            ["should be a date time"]}}}}
                  (-> valid-transaction
                      (assoc-in [:card :due-date] invalid-value)
                      (transaction/validate)))
        1                 :length
        10                :length
        :any              :type
        (java.util.Date.) :type
        "anything"        :type)))
  (testing "Should return nil when transaction is valid"
    (is (match? nil (transaction/validate valid-transaction)))))

(deftest create
  (testing "Return error wher receivind an invalid transaction"
    (are [transaction]
        (match? {:error
                 {:message "Invalid transaction."
                  :data map?}}
                (transaction/create (create-context) transaction))
      (dissoc valid-transaction :amount)
      (dissoc valid-transaction :card)
      (dissoc valid-transaction :method)))

  (testing "Return the entity with generated fields for credit card transaction"
    (let [ctx                                     (create-context)
          request                                 (-> valid-transaction
                                          (assoc :method :credit-card))
          start-timestamp                         (tick/date-time)
          {:transaction/keys [created-at
                              transaction-date
                              updated-at]
           :as               created-transaction} (transaction/create ctx request)]
      (is
       (match? {:transaction/amount           (:amount request)
                :transaction/method           :credit-card
                :transaction/card             {:card/number        (-> request :card :number)
                                               :card/holder-name   (-> request :card :holder-name)
                                               :card/security-code (-> request :card :security-code)
                                               :card/due-date      tick/date-time?}
                :transaction/transaction-date tick/date-time?
                :transaction/created-at       tick/date-time?
                :transaction/updated-at       tick/date-time}
               created-transaction)
       "should return a valid transaction entity with requested values")
      (is (tick/> (tick/date-time)
                  created-at
                  start-timestamp)
          "should have creation date setted for now")
      (is (tick/= created-at updated-at)
          "should have last update date setted for the same value as creation date")))

  (testing "Return the entity with generated fields for debit card transaction"
    (let [ctx                         (create-context)
          request                     (-> valid-transaction
                                          (assoc :method :debit-card))
          start-timestamp             (tick/date-time)
          {:transaction/keys [created-at
                              transaction-date
                              updated-at]
           :as               created-transaction} (transaction/create ctx request)]
      (is
       (match? #:transaction
               {:amount           (:amount request)
                :method           :debit-card
                :card             #:card{:number        (-> request :card :number)
                                         :holder-name   (-> request :card :holder-name)
                                         :security-code (-> request :card :security-code)
                                         :due-date      tick/date-time?}
                :transaction-date tick/date-time?
                :created-at       tick/date-time?
                :updated-at       tick/date-time}
               created-transaction)
       "should return a valid transaction entity with requested values")
      (is (tick/> (tick/date-time)
                  created-at
                  start-timestamp)
          "should have creation date setted for now")
      (is (tick/= created-at updated-at)
          "should have last update date setted for the same value as creation date"))))
