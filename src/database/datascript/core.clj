(ns database.datascript.core
  (:require [datascript.core :as ds]
            [database.core :as db]
            [tick.core :as tick]))

(def schema [{:db/unique      :db.unique/identity
              :db/ident       :card/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/doc         "Unique identification"}
             {:db/valueType   :db.type/string
              :db/ident       :card/number
              :db/cardinality :db.cardinality/one
              :db/doc         "Sixteen (16) digit card number"}
             {:db/valueType   :db.type/string
              :db/ident       :card/holder-name
              :db/cardinality :db.cardinality/one
              :db/doc         "Card holder name"}
             {:db/valueType   :db.type/string
              :db/ident       :card/security-code
              :db/cardinality :db.cardinality/one
              :db/doc         "Three (3) digit security code"}
             {:db/valueType   :db.type/instant
              :db/ident       :card/due-date
              :db/cardinality :db.cardinality/one
              :db/doc         "Validity due date"}
             {:db/unique      :db.unique/identity
              :db/ident       :transaction/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/doc         "Unique identification"}
             {:db/valueType   :db.type/keyword
              :db/ident       :transaction/method
              :db/cardinality :db.cardinality/one
              :db/doc         "Payment method"}
             {:db/valueType   :db.type/bigdec
              :db/ident       :transaction/amount
              :db/cardinality :db.cardinality/one
              :db/doc         "Related amount"}
             {:db/valueType   :db.type/instant
              :db/cardinality :db.cardinality/one
              :db/ident       :transaction/date
              :db/doc         "Transaction creation timestamp"}
             {:db/valueType   :db.type/instant
              :db/ident       :transaction/created-at
              :db/cardinality :db.cardinality/one
              :db/doc         "Entity creation timestamp"}
             {:db/valueType   :db.type/instant
              :db/ident       :transaction/updated-at
              :db/cardinality :db.cardinality/one
              :db/doc         "Last entity update timestamp"}
             {:db/valueType   :db.type/ref
              :db/ident       :transaction/card
              :db/cardinality :db.cardinality/one
              :db/doc         "Related card"}])

(defn- create-transaction! [{:keys [conn]} transaction]
  (ds/transact! conn [transaction]))

(defn ->MemoryDatabase [schema]
  (with-meta
    {:conn (ds/create-conn schema)}
    {`db/create-transaction! create-transaction!}))
