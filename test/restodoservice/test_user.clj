(ns restodoservice.test_user
  (:use clojure.test)
  (:require [restodoservice.user :as user]))

(deftest test-match-and-not-null
  (is (user/match-and-not-null "asd" "asd"))
  (is (not (user/match-and-not-null "asd" "asdf")))
  (is (not (user/match-and-not-null "asd" nil)))
  (is (not (user/match-and-not-null nil "asdf")))
  (is (not (user/match-and-not-null nil nil))))