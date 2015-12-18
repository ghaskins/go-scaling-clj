(ns go-scaling.core
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(require '[clojure.core.async :as async])
(use 'clojure.pprint)

(def cli-options
  ;; An option with a required argument
  [["-c" "--count COUNT" "The number of go routines to launch"
    :default 1000000
    :parse-fn #(Integer/parseInt %)
    :validate [#(> % 0)]]
   ["-h" "--help"]])

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn launchone [output]
  (let [input (async/chan)]
    (async/go (while (if-let [msg (async/<! input)]
                       (async/>! output msg)
                       nil)
                nil))
    input))

(defn launch [count]
  (let [input (async/chan 100)]
    (map (fn [i] (launchone input)) (range count))))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    ;; Handle help and error conditions
    (cond
      (:help options) (exit 0 summary))
    (let [count (:count options)]
      (println "Launching" count "go routines")
      (let [clients (launch count)]
        ;;(pprint clients)
        (dorun (map #(async/close! %) clients))
        ))))
