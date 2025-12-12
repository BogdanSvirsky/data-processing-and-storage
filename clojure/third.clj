(ns pfilter.core
(:require [clojure.test :refer :all]))

(defn pfilter
([pred coll] (pfilter pred coll 64))
([pred coll block-size]
(let [blocks (partition-all block-size coll)
futs (map (fn [blk] (future (doall (filter pred blk)))) blocks)]
(letfn [(go [fs]
(lazy-seq
(when-let [s (seq fs)]
(let [f (first s)]
(when (seq (rest s)) (first (rest s)))
(let [res @f]
(concat res (go (rest s))))))))]
(go futs)))))

(deftest pfilter-correctness
(is (= (seq (filter odd? (range 20)))
(seq (pfilter odd? (range 20) 5)))))

(deftest pfilter-infinite
(is (= (take 10 (filter odd? (range)))
(take 10 (pfilter odd? (range) 7)))))

(deftest pfilter-equals-filter
(let [coll (range 100)
pred (fn [x] (zero? (mod x 3)))]
(is (= (doall (filter pred coll))
(doall (pfilter pred coll 10))))))

(run-tests 'pfilter.core)

(println "\n--- Bench: comparing filter vs pfilter ---")
(let [n 200
heavy-pred (fn [x] (Thread/sleep 10) (even? x))
coll (range n)
block-size 10
t1 (System/nanoTime)
_ (doall (filter heavy-pred coll))
t2 (System/nanoTime)
seq-time-ms (/ (double (- t2 t1)) 1e6)
t3 (System/nanoTime)
_ (doall (pfilter heavy-pred coll block-size))
t4 (System/nanoTime)
pseq-time-ms (/ (double (- t4 t3)) 1e6)]
(println (format "filter   : %.1f ms" seq-time-ms))
(println (format "pfilter  : %.1f ms (block-size=%d)" pseq-time-ms block-size))
(shutdown-agents)