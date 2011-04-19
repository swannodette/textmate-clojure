#!/usr/bin/env cake run
(require '[clojure.java.io :as io])
           
(load-file (str (io/file (cake/*env* "TM_BUNDLE_SUPPORT") "utils.clj")))

(in-ns 'user)
(let [tm-filepath (cake/*env* "TM_FILEPATH")]
  (when (not (= tm-filepath ""))
    (textmate/attempt
      (load-file tm-filepath)
      (clojure.core/println "<pre>Loading finished.</pre>"))))