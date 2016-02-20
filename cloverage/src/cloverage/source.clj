(ns cloverage.source
  (:require [clojure.tools.reader :as r]
            [clojure.tools.reader.reader-types :as rt]))

(defn resource-path
  "Given a symbol representing a lib, return a classpath-relative path.  Borrowed from core.clj."
  [lib]
  (str (.. (name lib)
           (replace \- \_)
           (replace \. \/))
       ".clj"))

(defn resource-reader [resource]
  (if-let [resource (.getResourceAsStream
                     (clojure.lang.RT/baseLoader)
                     resource)]
    resource
    ;; try cljc if clj not found
    (if-let [resource (.getResourceAsStream
                       (clojure.lang.RT/baseLoader)
                       (str resource "c"))]
      resource
      (throw (IllegalArgumentException. (str "Cannot find resource " resource))))))

(defn form-reader [ns-symbol]
  (rt/indexing-push-back-reader
   (rt/input-stream-push-back-reader
    (resource-reader (resource-path ns-symbol)))))

(defn forms [ns-symbol]
  (let [src (form-reader ns-symbol)]
    (loop [forms nil]
      (if-let [form (r/read src false nil)]
        (recur (conj forms form))
        (reverse forms)))))

(defn ns-form [ns-symbol]
  (let [src (form-reader ns-symbol)]
    (first (drop-while #(not= 'ns (first %))
                       (take-while (comp not nil?)
                                   (repeatedly #(r/read {:eof nil
                                                         :features #{:clj}
                                                         :read-cond :allow}
                                                        src)))))))
