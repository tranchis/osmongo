(ns osmongo.osm2mongo
  (:require [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [somnium.congomongo :as m]))

(defn to-long [st]
  (if (or (nil? st) (empty? st))
    nil
    (java.lang.Long/parseLong st)))

(defn to-boolean [st]
  (if (or (nil? st) (empty? st))
    nil
    (java.lang.Boolean/parseBoolean st)))

(defn common-attrs [attrs]
  {:id (to-long (:id attrs))
   :version (to-long (:version attrs))
   :timestamp (:timestamp attrs)
   :uid (to-long (:uid attrs))
   :user (:user attrs)
   :changeset (:changeset attrs)})

(defmulti treat-node :tag)

(defmethod treat-node :nd [node]
  (let [attrs (:attrs node)
        ref (to-long (:ref attrs))]
    ref))

(defmethod treat-node :member [node]
  (let [attrs (:attrs node)
        type (:type attrs)
        ref (to-long (:ref attrs))
        role (:role attrs)]
    (if (not (= (into #{} (keys attrs)) (into #{} [:ref :role :type])))
      (do
        (println (keys attrs))
        (System/exit 1)))
    {:type type :ref ref :role role}))

(defmethod treat-node :tag [node]
  (let [attrs (:attrs node)
        k (:k attrs)
        v (:v attrs)]
    {(keyword k) [v]}))

(defmethod treat-node :bounds [node]
  ;; Do nothing, not treating it for mongodb
  nil)

(defmethod treat-node :way [node]
  "Example way:
		<way id=\"5090250\" visible=\"true\"
         timestamp=\"2009-01-19T19:07:25Z\" version=\"8\"
         changeset=\"816806\" user=\"Blumpsy\" uid=\"64226\">
		    <nd ref=\"822403\"/>
		    <nd ref=\"21533912\"/>
		    <nd ref=\"821601\"/>
		    <nd ref=\"21533910\"/>
		    <nd ref=\"135791608\"/>
		    <nd ref=\"333725784\"/>
		    <nd ref=\"333725781\"/>
		    <nd ref=\"333725774\"/>
		    <nd ref=\"333725776\"/>
		    <nd ref=\"823771\"/>
		    <tag k=\"highway\" v=\"residential\"/>
		    <tag k=\"name\" v=\"Clipstone Street\"/>
		    <tag k=\"oneway\" v=\"yes\"/>
    </way>"
  #_(let [attrs (:attrs node)
         common (common-attrs attrs)
         visible (to-boolean (:visible attrs))
         values (map treat-node (:content node))
         values-separated (group-by number? values)
         nds (get values-separated true)
         tags (apply merge-with concat (get values-separated false))
         result (merge common {:visible visible
                              :nodes nds :tags tags})]
     (m/insert! :ways result))
  nil)

(defmethod treat-node :relation [node]
  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
   <relation id=\"10733\" version=\"3\" timestamp=\"2008-10-10T18:20:23Z\"
             uid=\"70696\" user=\"xybot\" changeset=\"238039\">
     <member type=\"way\" ref=\"23685458\" role=\"forward\"/>
     <tag k=\"created_by\" v=\"xybot\"/>
     <tag k=\"network\" v=\"Transports Metropolitans de Barcelona\"/>
     <tag k=\"operator\" v=\"TMB\"/>
     <tag k=\"ref\" v=\"39\"/>
     <tag k=\"route\" v=\"bus\"/>
     <tag k=\"type\" v=\"route\"/>
     </tag>
   </relation>"
  (let [attrs (:attrs node)
        common (common-attrs attrs)
        values (map treat-node (:content node))
        values-separated (group-by #(contains? % :role) values)
        tags (apply merge-with concat (get values-separated false))
        members (apply merge (get values-separated true))
        result (merge common {:tags tags :members members})]
    (m/insert! :relations result)))

(defmethod treat-node :node [node]
  "Example node:
   ```xml
   <?xml version=\"1.0\" encoding=\"UTF-8\"?>
   <node id=\"8773782\" version=\"4\" timestamp=\"2013-04-01T17:35:37Z\"
         uid=\"605366\" user=\"EliziR\" changeset=\"15571558\"
         lat=\"41.222826\" lon=\"1.7352535\">
     <tag k=\"highway\" v=\"traffic_signals\"/>
   </node>
   ```"
  #_(let [attrs (:attrs node)
         common (common-attrs attrs)
         lat (:lat attrs)
         lon (:lon attrs)
         tags (apply merge-with concat (map treat-node (:content node)))
         result (merge common {:geometry {:lat lat :lon lon} :tags tags})]
     (m/insert! :nodes result))
  nil)

(defn osm-to-mongo
  "Usually there are four node types inside the OSM
   file: `#{:bounds :way :node :relation}`."
  [file-name mongo-url]
  (m/set-connection! (m/make-connection mongo-url))
  (m/destroy! :nodes {})
  (m/destroy! :ways {})
  (m/destroy! :relations {})
  (with-open [rdr (io/reader file-name)]
    (let [contents (xml/parse rdr)
          elems (:content contents)]
      (dorun (map treat-node elems)))))

