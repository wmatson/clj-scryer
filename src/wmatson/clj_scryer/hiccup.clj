(ns wmatson.clj-scryer.hiccup
  (:require [hiccup.core :as hccp]
            [orchard.inspect :as ori]))

(defn- iline->hiccup [line]
  (reduce (fn [acc [k display idx :as next]]
            (conj acc (if (#{:value} k)
                        [:div {:style {:border "1px solid black"}}
                         display [:button ">"]]
                        next)))
          [:p] line))

(defn- irender->hiccup [inspector-rendered]
  (conj [:div [:p [:button "Prev"] [:button "Up"] [:button "Next"]]]
        (->> inspector-rendered
             (partition-by (comp #{:newline} first))
             (partition-all 1 2)
             (apply concat)
             (map iline->hiccup))))

(defn inspect [value]
  (let [hiccup (irender->hiccup (:rendered (ori/start (ori/fresh) value)))
        temp-file (java.io.File/createTempFile "inspect" ".html")]
    (spit temp-file (hccp/html hiccup))
    (clojure.java.browse/browse-url (.. temp-file toURI toString))))

(comment
  (inspect (map #(hash-map :num %) (range))))