(defn permutations [alphabet n]
  (let [initial (map str alphabet)]
    (if (= n 1)
      initial
      (reduce
       (fn [acc _]
         (mapcat
          (fn [s]
            (map (fn [ch]
                   (str s ch))
                 (filter #(not= (last s) %) alphabet)))
          acc))
       initial
       (range (dec n))))))


(println (permutations ["a" "b" "c"] 2))
