(ns me-paga-ai.transactions.core
  (:require [me-paga-ai.transactions.schema :as tx-schema]
            [malli.core :as malli]
            [malli.error :as malli-err]
            [tick.core :as tick])
  (:import java.util.UUID))


(defn validate [transaction]
  (if (malli/validate tx-schema/Transaction transaction)
    nil
    {:error {:message  "Invalid transaction."
             :data (->> transaction
                        (malli/explain tx-schema/Transaction)
                        (malli-err/humanize))}}))
