(ns osmongo.queries
  (:require [osmongo.db :as db]
            [somnium.congomongo :as m]))

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

(coordinate->osmid 41.377873 2.152926 nil)
