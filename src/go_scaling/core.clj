(ns go-scaling.core
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(require '[clojure.core.async :as async])
(use 'clojure.pprint)

(def cli-options
  ;; An option with a required argument
  [["-c" "--count COUNT" "The number of go routines to launch"
    :default 100000
    :parse-fn #(Integer/parseInt %)
    :validate [#(> % 0)]]
   ["-h" "--help"]])

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn asyncecho [output]
  (let [input (async/chan)]
    (async/go (while (if-let [msg (async/<! input)]
                       (async/>! output msg)
                       (async/close! output))
                nil))
    input))

(defn launch [input count]
  ;; note the inversion of input to output in asyncecho
  (into {} (map (fn [i] (vector i (asyncecho input))) (range count))))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    ;; Handle help and error conditions
    (cond
      (:help options) (exit 0 summary))
    (let [count (:count options)]
      (println "Launching" count "go routines")
      (let [input (async/chan 100)
            clients (launch input count)]
        ;;(pprint clients)
        (dorun (map (fn client [[index output]]

                      ;; close the channel, which should cause the server to exit
                      (async/close! output)
                      ;; synchronize with the exit
                      (async/<!! input)
                      )
                    clients))))))
