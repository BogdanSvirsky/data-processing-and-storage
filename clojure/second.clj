(ns primes.core
  (:require [clojure.test :refer :all]))

(defn- sieve [s]
  (lazy-seq
    (when-let [s (seq s)]
      (let [p (first s)]
        (cons p (sieve (remove #(zero? (mod % p)) (rest s))))))))

(def primes
  (sieve (iterate inc 2)))

(deftest test-first-10-primes
  (is (= (take 10 primes) [2 3 5 7 11 13 17 19 23 29])))

(deftest test-100th-prime
  (is (= (nth primes 99) 541)))

(deftest test-first-prime
  (is (= (first primes) 2)))

(run-tests 'primes.core)
