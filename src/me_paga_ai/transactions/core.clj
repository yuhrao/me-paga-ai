(ns me-paga-ai.transactions.core
  (:require [me-paga-ai.transactions.schema :as tx-schema]
            [malli.core :as malli]
            [malli.error :as malli-err]
            [tick.core :as tick]
            [database.core :as db]
            [database.datascript.core :as ds])
  (:import java.util.UUID))


(defn validate [transaction]
  (if (malli/validate tx-schema/Transaction transaction)
    nil
    {:error {:message  "Invalid transaction."
             :data (->> transaction
                        (malli/explain tx-schema/Transaction)
                        (malli-err/humanize))}}))
(defn qualify-map [m n]
  (reduce-kv (fn [acm k v]
               (assoc acm (keyword (name n) (name k)) v)) {} m))

(defn qualify-transaction [transaction]
  (-> transaction
      (update :card #(qualify-map %1 :card))
      (qualify-map "transaction")))

(defn create [{:keys [repo] :as ctx} transaction]
  (if-let [error (validate transaction)]
    error
    (->> transaction
         qualify-transaction
         (db/create-transaction! repo))))
