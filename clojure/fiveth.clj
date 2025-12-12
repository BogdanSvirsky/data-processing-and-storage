(ns dining-philosophers)

(def num-philosophers 5)
(def think-ms 5)
(def eat-ms 5)
(def rounds 100)

(def txn-attempts (atom 0))
(def forks (vec (repeatedly num-philosophers #(ref 0))))

(defn philosopher [left-fork right-fork]
  (future
    (dotimes [_ rounds]
      (Thread/sleep think-ms)
      (dosync
        (swap! txn-attempts inc)
        (alter left-fork inc)
        (alter right-fork inc)
        (Thread/sleep eat-ms)))))

(defn -main []
  (println "Starting simulation...")
  (let [start (System/currentTimeMillis)
        threads (doall
                  (for [i (range num-philosophers)]
                    (let [left (nth forks i)
                          right (nth forks (mod (inc i) num-philosophers))]
                      (philosopher left right))))]
    
    (doseq [t threads] @t)
    
    (let [end (System/currentTimeMillis)]
      (println "--------------------------------------------------")
      (println "Configuration:" {:philosophers num-philosophers
                                 :think-ms think-ms
                                 :eat-ms eat-ms
                                 :rounds rounds})
      (println "Total Execution Time:" (- end start) "ms")
      (println "Total Transaction Attempts:" @txn-attempts)
      (println "Fork Usage Counts:" (map deref forks))
      (println "--------------------------------------------------")
      (shutdown-agents))))

(-main)