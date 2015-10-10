(ns restodoservice.test_todo
  (:use clojure.test)
  (:require [restodoservice.todo :as todo]))

(deftest test-zip-map-to-array
  (is (= {} (todo/zip-map-to-array [])))
  (is (= {} (todo/zip-map-to-array nil)))
  (is (= {"asd" 123} (todo/zip-map-to-array ["asd" 123])))
  (is (= {"trew" 5432 543 "qxcd"} (todo/zip-map-to-array ["trew" 5432 543 "qxcd"]))))
