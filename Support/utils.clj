(ns textmate
 (:require [clojure.string :as string]
           [clojure.java.io :as io]
           [clojure.stacktrace :as stacktrace]
           [clojure.contrib.seq-utils :as seq-utils]
           [clojure.contrib.pprint :as pprint]))
(clojure.core/refer 'clojure.core)

(defn htmlize [#^String text]
  (-> text
      (.replaceAll "&"  "&amp;")
      (.replaceAll "<"  "&lt;")
      (.replaceAll ">"  "&gt;")
      (.replaceAll "\n" "<br>")))

(defn pprint-str [x]
  (with-out-str (pprint/pprint x)))

(defn str-nil [o]
  (if o (str o) "nil"))

(defn ppstr-nil [o]
  (if o (pprint-str o) "nil"))

(defn escape-quotes [#^String s]
  (-> s
     (.replaceAll "\"" "\\\"")))

(defn escape-characters [#^String s]
 (let [#^java.util.regex.Matcher m (.matcher #"\\(\S)" s)]
    (if (.matches m)
      (.replaceAll m "\\\\$1")
      s)))

(defn escape-str [s]
  (-> s #_escape-characters escape-quotes))

(defn print-stack-trace [exc]
  (if-let [cause (.getCause exc)]
    (print-stack-trace cause)
    (do
      (println (.getMessage exc))
      (doall (map #(println (.toString %)) (seq (.getStackTrace exc)))))))

(defn add-source-links-to-exception-dump
  "finds clojure files in stacktrace and adds txtmt links"
  [exception-dump]
  (string/replace exception-dump
    #"\((.*?\.clj):(\d+)\)"
    (fn [[orig file line-num]]
      (let [resource (ClassLoader/getSystemResource file)]
        (if (and (not (nil? resource)) (not (empty? (.getFile resource))))
          (format "(<a href=\"txmt://open?line=%s&url=file:///%s\">%s:%s</a>)"
            line-num (.getFile resource) file  line-num)
          orig)))))


(defmacro attempt [& body]
  `(try
     (do
       ~@body)
     (catch Exception e#
       (clojure.core/println
            "<h1>Exception:</h1>"
            "<pre>"
            (add-source-links-to-exception-dump (with-out-str (print-stack-trace e#)))
            "</pre>"))))

(defn reader-empty? [#^java.io.PushbackReader rdr]
  (let [ch (.read rdr)]
    (do (.unread rdr ch)
        (= ch -1))))

(defn text-forms
  "Uses Clojure compiler to return a (lazy) seq of forms from text,
   each form which yielded a parsing error returns a nil.
   If text consists of a single well-formed sexpr, this should
   return a single non-nil form."
  [t]
  (let [rdr (-> t java.io.StringReader. java.io.PushbackReader.)]
   (for [_ (repeat nil) :while (not (reader-empty? rdr))]
      (try (read rdr) (catch Exception _ nil)))))

(defn file-ns
  "Find the namespace of a file; searches for the first ns  (or in-ns)
   form in the file and returns that symbol. Defaults to 'user if one
   can't be found"
  []
  (let [forms
          (-> (cake/*env* "TM_FILEPATH")
              slurp
              text-forms)
        ns-form?
          (fn [f] (and (seq? f)
                        (#{"ns" "in-ns"} (str (first f)))))
        [ns-fn ns]
          (first
            (for [f forms :when (ns-form? f)]
              [(first f) (second f)]))]
    (if ns
      (if (= (str ns-fn) "ns") ns (eval ns))
      'user)))

(defn enter-ns
  "Enter a ns, wrapped for debugging purposes"
  [ns]
  #_(println (str "Entering " ns))
  (in-ns ns))

(defn enter-file-ns
  "Enter the ns of the file"
  []
  (let [ns (file-ns)]
    (enter-ns ns)))

(defmacro eval-in-file-ns
  "For the current file, enter the ns (if any)
  and evaluate the form in that ns, then pop
  back up to the original ns"
  [form]
  `(let [old-ns# *ns*]
    (enter-file-ns)
    (let [r# (eval ~form)]
      (enter-ns (-> old-ns# str symbol))
      r#)))

(defn project-relative-src-path []
   (let [user-dir (str (cake/*env* "TM_PROJECT_DIRECTORY") "/src/")
        path-to-file (string/replace (cake/*env* "TM_FILEPATH")  user-dir "")]
  path-to-file))

(defn carret-info
  "returns [path line-index column-index] info
   about current location of cursor"
  []
  [(cake/*env* "TM_FILEPATH")
    (dec (Integer/parseInt (cake/*env* "TM_LINE_NUMBER")))
    (dec (Integer/parseInt (cake/*env* "TM_COLUMN_NUMBER")))])

(defn text-before-carret []
  (let [[path,line-index,column-index] (carret-info)
        lines (-> path io/reader line-seq)
        #^String last-line (nth lines line-index)]
     (apply str
       (apply str (for [l (take line-index lines)] (str l "\n")))
       (.substring last-line 0 (min column-index (.length last-line))))))

(defn text-after-carret []
 (let [[path,line-index,column-index] (carret-info)
       lines (-> path io/reader line-seq)]
    (apply str
      (.substring #^String (nth lines line-index) column-index)
      (apply str (for [l (drop (inc line-index) lines)] (str l "\n"))))))

(defn symbol-char?
  [c]
  (or (Character/isLetterOrDigit c) ((hash-set \_ \! \. \? \- \/) c)))

(defn get-symbol-to-autocomplete []
  (let [#^String line (-> "TM_CURRENT_LINE" cake/*env* escape-str)
        stop (dec (last (carret-info)))]
    #_(println (carret-info))
    (loop [index stop]            
      (let [ch (.charAt line index)]
        #_(println "index:" index "char:" ch"<br>")
        (cond 
            (neg? index) (.trim (.substring line 0 (inc stop)))
            (or (nil? ch) (not (symbol-char? (.charAt line index))))                                 
              (.trim (.substring line (inc index) (inc stop)))
            :else (recur (dec index)))))))


(defn get-current-symbol-str
  "Get the string of the current symbol of the cursor"
  []
  (let [#^String line (-> "TM_CURRENT_LINE" cake/*env* escape-str)
        index    (int (last (carret-info)))
        symbol-index?
          (fn [index]
            (and (< index (.length line))
                 (let [c (.charAt line index)] (symbol-char? c))))
        symbol-start
          (loop [i index]
            (if (or (= i 0) (-> i dec symbol-index? not))
              i (recur (dec i))))
        symbol-stop
          (loop [i index]
            (if (or (= i (inc (.length line))) (not (symbol-index? (inc i))))
              i (recur (inc i))))]
    (-> line
        (.substring symbol-start (min (.length line) (inc symbol-stop)))
        (.split "\\s+")
        first
        .trim)))

(defn get-current-symbol
  "Get current (selected) symbol. Enters file ns"
  []
  (ns-resolve  (enter-file-ns) (symbol (get-current-symbol-str))))



(defn find-last-delim [#^String t]
  (let [c (last t)]
    (cond
        ((hash-set \) \] \} \") c)  c
        ((hash-set \( \[ \{ \") c)
          (throw (RuntimeException.
            (str "Not a valid form ending in '" c "'")))
        :default :symbol)))

(defn indices-of [#^String t #^Character target]
  (reverse (for [[i c] (seq-utils/indexed t)
          :when (= c target)] i)))

(def matching-delims
  { \) \(
    \] \[
    \} \{
    \" \" })

(defn find-last-sexpr [#^String t]
  (let [t (.trim t)
        d (find-last-delim t)]
    #_(do (println (htmlize (str "Input: " t)))
          (println (htmlize (str "last delim: " d))))
    (if (= :symbol d)
      (get-current-symbol)
      (first
        (filter identity
           (for [i (indices-of t (matching-delims d))]
                  (let [cur (.substring t i)]
                    #_(println (htmlize (str "search: " i " " cur)))
                    (try
                      (let [forms (text-forms cur)]
                        #_(println (htmlize (str "forms: " forms)))
                        (when (= (count forms) 1)
                          (first forms)))
                      (catch Exception _ nil)))))))))

(defn display-form-eval [form]
  (clojure.core/println
      "<h1>Form</h1>"
      "<pre>"(textmate/ppstr-nil form)"</pre>")
  (clojure.core/println
      "<h1>Result</h1>"
      "<pre>"
      (textmate/attempt
        (-> form
            textmate/eval-in-file-ns
            textmate/ppstr-nil
            textmate/htmlize
            .trim))
      "</pre>"))

(defn get-last-sexpr
  "Get last sexpr before carret"
  []
  (-> (text-before-carret) find-last-sexpr))

(defn get-selected-sexpr
  "Get highlighted sexpr"
  []
  (-> "TM_SELECTED_TEXT" cake/*env* escape-str clojure.core/read-string))