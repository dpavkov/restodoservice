(defproject restodoservice "0.1-SNAPSHOT"
  :description "Exposes rest services"
  :plugins [[lein-ring "0.8.11"]]
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring "1.3.2"]
                 [liberator "0.13"]
                 [compojure "1.3.4"]
                 [com.taoensso/carmine "2.12.0"]
                 [com.draines/postal "1.11.3"]]
  :javac-options ["-target" "1.8" "-source" "1.8"]
  :aot [restodoservice.core]
  :main restodoservice.core
  :ring {:handler restodoservice.core/app})
