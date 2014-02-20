(ns osmongo.db
  (:require [snippets-generic :as cs]
            [somnium.congomongo :as m]))

(def props
  "Properties taken from `aggregator.properties`. These include:

  * Information (e.g., name, coordinates) about the city to crawl.
  * Access configuration to access the `mongo` instance."
  (cs/load-props "db.properties"))

(def mongo-uri
  "The URI identifying the `mongo` connection. It is used by other
   namespaces, such as `atalaya.api.atalaya-api`."
  (str "mongodb://" (:mongo.user props) ":" (:mongo.pass props) "@"
       (:mongo.host props) ":" (:mongo.port props) "/" (:mongo.db props)))

(def conn
  "Object with the permanent open connection with the `mongo` instance."
  (try
    (m/make-connection mongo-uri)
    (catch Exception e
      nil)))
