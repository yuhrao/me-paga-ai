(ns database.datascript.core
  (:require [datascript.core :as ds]
            [database.core :as db]
            [tick.core :as tick])
  (:import java.util.UUID))

(defn- create-transaction! [{:keys [conn]} transaction]
  (let [now (tick/date-time)
        new-transaction (-> transaction
                            (assoc
                             :transaction/id (UUID/randomUUID)
                             :transaction/transaction-date now
                             :transaction/created-at now
                             :transaction/updated-at now))]
    (ds/transact! conn [new-transaction])
    new-transaction))

(defn ->MemoryDatabase []
  (with-meta
    {:conn (ds/create-conn)}
    {`db/create-transaction! create-transaction!}))
