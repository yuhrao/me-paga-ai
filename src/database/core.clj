(ns database.core)

(defprotocol Database
  "Persistence abstraction"
  :extend-via-metadata true
  (create-transaction! [db transaction] "Create a new transaction"))
