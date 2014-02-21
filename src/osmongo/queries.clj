(ns osmongo.queries
  (:require [osmongo.db :as db]
            [somnium.congomongo :as m]
            [clojure.data.json :as json]))

(defn node->way [node]
  (contains?
    (apply merge (map :tags (m/fetch :ways :where {:nodes node})))
    :highway))

(defn coordinate->nodes [lat lng dist]
  (:results (m/command {:geoNear "nodes"
                        :near [lng lat]
                        :spherical true
                        :maxDistance dist
                        :distanceMultiplier 6378.137})))

(defn types->vector [types]
  (if (nil? types)
    []
    (if (vector? types)
      types
      [types])))

(defn coordinate->osmid [lat lng types]
  (m/with-mongo db/conn
    (loop [dist 0.000001 node nil]
      (if (not (nil? node))
        node
        (let [v-types (types->vector types)
              results (coordinate->nodes lat lng dist)
              results-ways (filter node->way (map #(:_id (:obj %)) results))]
          (recur (* 10 dist) (first results-ways)))))))

(defn point->osmid [point]
  (coordinate->osmid (second point) (first point) nil))

(defmulti geometry->osmid :type)

(defmethod geometry->osmid "Point" [geometry]
  [(point->osmid (:coordinates geometry))])

(defmethod geometry->osmid "LineString" [geometry]
  (into [] (map point->osmid (:coordinates geometry))))

(defmulti geoclj->osmid :type)

(defmethod geoclj->osmid "FeatureCollection" [geoclj]
  (assoc geoclj :features (into [] (map geoclj->osmid (:features geoclj)))))

(defmethod geoclj->osmid "Feature" [geoclj]
  (assoc-in geoclj [:properties :osm-ids] (geometry->osmid (:geometry geoclj))))

(defn geojson->osmid [geojson]
  (let [attached (geoclj->osmid (json/read-str geojson :key-fn keyword))]
    (json/write-str attached)))
