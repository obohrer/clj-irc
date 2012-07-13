(ns clj-irc.utils)

(defn try-times* [f times]
  (let [res (->> (fn[]
                   (try (f)
                     (catch Exception e ::fail)))
                 (repeatedly times)
                 (drop-while #{::fail})
                 first)]
    (when-not (= ::fail res)
      res)))