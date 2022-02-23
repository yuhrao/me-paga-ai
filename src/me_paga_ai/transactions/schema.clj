(ns me-paga-ai.transactions.schema
  (:require [malli.core :as malli]
            [tick.core :as tick]))

(def DateTime
  (malli/-simple-schema
   {:type :date-time
    :pred tick/date-time?
    :type-properties {:error/message "should be a date time"
                      :decode/string #(tick/format :iso-local-date-time %1)}}))

(def TransactionCard
  (malli/schema
   [:map
    [:number [:string {:min 16 :max 16}]]
    [:holder-name [:string {:min 3}]]
    [:security-code [:int {:min 100 :max 999}]]
    [:due-date DateTime]]))

(def Transaction
  (malli/schema
   [:map
    [:amount decimal?]
    [:method [:enum :debit-card :credit-card]]
    [:card TransactionCard]]))
