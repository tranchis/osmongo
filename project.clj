(defproject osmongo "0.1.0-SNAPSHOT"
  :description ""
  :url "http://example.com/FIXME"
  :repositories [["bintray" "http://dl.bintray.com/tranchis/clojure-snippets"]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-time "0.6.0"]
                 [congomongo "0.4.1"]
                 [org.clojure/data.xml "0.0.7"]]
  :plugins [[no-man-is-an-island/lein-eclipse "2.0.0"]
            [lein-marginalia "0.7.1"]]
  :main osmongo.osm2mongo
  :aot [osmongo.osm2mongo]
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]]}})
