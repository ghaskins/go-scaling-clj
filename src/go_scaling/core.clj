(ns go-scaling.core
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [clojure.core.async :as async :refer [<! >! <!! >!! close! chan]])
  (:require [clojure.pprint :refer :all])
  (:gen-class))


(def cli-options
  ;; An option with a required argument
  [["-c" "--count COUNT" "The number of go routines to launch"
    :default 100000
    :parse-fn #(Integer/parseInt %)
    :validate [#(> % 0)]]
   ["-h" "--help"]
   ["-v" "--verbose"]])

(defn asyncecho [output]
  (let [input (chan)]
    (async/go (while (when-let [msg (<! input)]
                       (>! output msg)))
              (close! output))
    input))

(defn launch [input count]
  ;; note the inversion of input to output in asyncecho
  (mapv (fn [i] (vector i (asyncecho input))) (range count)))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    ;; Handle help and error conditions
    (cond
      (:help options) (println summary)
      errors (println errors)
      :else (let [count (:count options)]
               (println "Launching" count "go routines")
               (let [input   (chan 100)
                     clients (launch input count)]
                 (when (:verbose options)
                   (pprint clients))
                 (dorun (map (fn [ [index output] ]
                               ;; close the channel which should cause the server to exit
                               (when (options :verbose)
                                 (println "closing channel" index))
                               (close! output)
                               ;; synchronize with exit
                               (when (options :verbose)
                                 (println "waiting for channel " index "to exit"))
                               (<!! input))
                             clients)))))))
